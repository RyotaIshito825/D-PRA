package com.ishito.sample.dpra.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.ishito.sample.dpra.constants.ErrorKinds;
import com.ishito.sample.dpra.constants.ErrorMessage;
import com.ishito.sample.dpra.entity.Car;
import com.ishito.sample.dpra.entity.Car.Classification;
import com.ishito.sample.dpra.entity.Car.JapaneseCalendar;
import com.ishito.sample.dpra.entity.Car.VehicleInspection;
import com.ishito.sample.dpra.entity.PriceCard;
import com.ishito.sample.dpra.service.CarsService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;


@Controller
@RequestMapping("/cars")
public class CarsController {
    
    private final CarsService carsService;
    // private final PriceCardService priceCardService;

    @Autowired
    public CarsController(CarsService carsService) {
        this.carsService = carsService;
    }

    @GetMapping(value = "/top")
    public String pageTop() {
        return "cars/top";
    }

    @GetMapping(value = "/list")
    public String top(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "minPrice" , required = false) Integer minPrice,
        @RequestParam(name = "maxPrice", required = false) Integer maxPrice,
        @RequestParam(name = "page", defaultValue = "0") int page,
        Model model,
        HttpSession session) {


        List<Car> carList = (List<Car>) session.getAttribute("carList");

        String shopName = (String) session.getAttribute("shopName");
        String shopImageBase64 = (String) session.getAttribute("shopImageBase64");

        PriceCard priceCard = (PriceCard) session.getAttribute("priceCard");
        if (priceCard != null) {
            model.addAttribute("priceCardName", priceCard.getPriceCardName());
            session.setAttribute("priceCardName", priceCard.getPriceCardName());
        } else {
            priceCard = new PriceCard();
        }

        if (keyword != null) {
            int min_price = (minPrice != null) ? minPrice : 0;
            int max_price = (maxPrice != null) ? maxPrice : 9999;

            carList = carsService.filterCarsByKeyword(carList, keyword, min_price, max_price);

            session.setAttribute("keyword", keyword);

            model.addAttribute("keyword", keyword);


            if (maxPrice != null && maxPrice != 0) model.addAttribute("maxPrice", max_price);
        }

        if (carList == null) {
            session.setAttribute("carList", new ArrayList<>());
            model.addAttribute("carList", carList);
            return "cars/list";
        }

        model.addAttribute("carList", carList);

        int size = 8;
        int start = page * size;
        int end = Math.min(start + size, carList.size());
        List<Car> pageList = new ArrayList<>(carList.subList(start, end));
        int totalPages = (int) Math.ceil((double) carList.size() / size);
        model.addAttribute("currentPage", page);
        // model.addAttribute("carsSize", pageList.size());
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("carList", pageList);

        return "cars/list";
    }

    // 車両新規登録画面に遷移
    @GetMapping(value = "/add")
    public String create(@ModelAttribute Car car, Model model) {

        // あとで消す
        car.setMaker("ダイハツ");
        car.setCarModel("タントカスタム");
        car.setGrade("RS 4WD");
        car.setRegistrationYear(7);
        car.setRegistrationMonth(10);
        
        car.setVehicleInspectionStatus(Car.VehicleInspection.YES);
        car.setViJc(Car.JapaneseCalendar.REIWA);
        car.setViYear("10");
        car.setViMonth("10");

        car.setPrice(111);
        car.setPriceDpf(1);
        car.setTotalPrice(222);
        car.setTotalPriceDpf(2);
        car.setMileage(1000);
        // あとで消す

        return "cars/new";
    }

    // 新規車両登録処理
    @SuppressWarnings("unchecked")
    @PostMapping(value = "/add/submit")
    public String add(@Valid @ModelAttribute Car car, BindingResult res, Model model, HttpSession session) {

        // あとで月とかのエラーチェックもやる
        try {
            int registrationYear = Integer.parseInt(String.valueOf(car.getRegistrationYear()));
        } catch (NumberFormatException e) {
            model.addAttribute("isNumberCheckRegistrationYearError", ErrorMessage.getErrorValue(ErrorKinds.ISNUMBERCHECK_REGISTRATIONYEAR_ERROR));
        }

        if (res.hasErrors()) {
            return "cars/new";
        }

        // 諸費用のとろこの計算
        BigDecimal carPrice = new BigDecimal(car.getPrice());
        BigDecimal carPriceDpf = new BigDecimal(car.getPriceDpf()).divide(new BigDecimal(10));
        BigDecimal carTotalPrice = new BigDecimal(car.getTotalPrice());
        BigDecimal carTotalPriceDpf = new BigDecimal(car.getTotalPriceDpf()).divide(new BigDecimal(10));
        BigDecimal carPriceAll = carPrice.add(carPriceDpf);
        BigDecimal carTotalPriceAll = carTotalPrice.add(carTotalPriceDpf);

        BigDecimal differenceAmount = carTotalPriceAll.subtract(carPriceAll);
        BigDecimal decimalPart = differenceAmount.remainder(BigDecimal.ONE);

        if (!carsService.isTotalPriceHigher(differenceAmount)) {
            return "cars/new";
        }

        try {

            Car newCar = car;
            List<Car> carList = (List<Car>) session.getAttribute("carList");

            if (carList == null || carList.isEmpty()) {
                newCar.setId(1);
            } else {

                int maxId = carList.stream()
                    .mapToInt(Car::getId)
                    .max()
                    .orElse(1);

                newCar.setId(maxId + 1);
            }

            int calcPriceOfInt = differenceAmount.intValue();
            int calcPriceOfDpf = decimalPart.multiply(new BigDecimal(10)).intValue(); 
            newCar.setCalcPriceOfInt(calcPriceOfInt); // 諸費用の整数部分のセット
            newCar.setCalcPriceOfDpf(calcPriceOfDpf); // 諸費用の小数部分のセット
            newCar.setViYear(
                toInt(newCar.getViYear()) < 1 || toInt(newCar.getViYear()) > 12
                ? ""
                : newCar.getViYear()
            );
            newCar.setViMonth(
                toInt(newCar.getViMonth()) < 1 || toInt(newCar.getViMonth()) > 12
                ? ""
                : newCar.getViMonth()
            );

            carList = (List<Car>) session.getAttribute("carList");
            ErrorKinds result = carsService.createCar(newCar);
            
            if (result == ErrorKinds.SUCCESS) {
                if (carList == null) {
                    carList = new ArrayList<>();
                }
                carList.add(newCar);
            }

            session.setAttribute("carList", carList);

            return "redirect:/cars/list";

        } catch (NumberFormatException e) {
            return "cars/new";
        }
    }

    // 車両更新画面表示
    @GetMapping(value = "/update")
    public String edit(@RequestParam(name = "index") int index, Model model, HttpSession session) {
        
        List<Car> carList = (List<Car>) session.getAttribute("carList");
        if (carList == null || carList.isEmpty()) {
            return "redirect:/cars/list";
        }

        Car car = carList.stream()
            .filter(c -> c.getId().equals(index))
            .findFirst()
            .orElse(null);

        model.addAttribute("index", index);
        model.addAttribute("car", car);
        
        return "cars/edit";
    }

    // 車両更新処理
    @PostMapping(value = "/update/submit")
    public String update(@Validated Car car, @RequestParam(name = "index") int index, BindingResult res, Model model, HttpSession session) {

        System.out.println("index : " + index);
        System.out.println("viyear : " + car.getViYear());

        List<Car> carList = (List<Car>) session.getAttribute("carList");
        Car updateCar = carList.stream()
            .filter(targetCar -> targetCar.getId() ==  Integer.valueOf(index))
            .findFirst()
            .orElse(null);

        updateCar.setId(index);
        updateCar.setMaker(car.getMaker());
        updateCar.setCarModel(car.getCarModel());
        updateCar.setGrade(car.getGrade());
        updateCar.setClassification(car.getClassification());
        updateCar.setFirstJc(car.getFirstJc());
        updateCar.setRegistrationYear(car.getRegistrationYear());
        updateCar.setRegistrationMonth(car.getRegistrationMonth());
        updateCar.setVehicleInspectionStatus(car.getVehicleInspectionStatus());
        updateCar.setViJc(car.getViJc());
        updateCar.setViYear(
            toInt(car.getViYear()) < 1 || toInt(car.getViYear()) > 12
            ? ""
            : car.getViYear()
        );
        updateCar.setViMonth(
            toInt(car.getViMonth()) < 1 || toInt(car.getViMonth()) > 12
            ? ""
            : car.getViMonth()
        );
        updateCar.setPrice(car.getPrice());
        updateCar.setPriceDpf(car.getPriceDpf());
        updateCar.setTotalPrice(car.getTotalPrice());
        updateCar.setTotalPriceDpf(car.getTotalPriceDpf());

        BigDecimal differentAmount = carsService.calculateFees(
            car.getPrice(),
            car.getPriceDpf(),
            car.getTotalPrice(),
            car.getTotalPriceDpf()
        );

        if (!carsService.isTotalPriceHigher(differentAmount)) {
            model.addAttribute("index", index);
            return "cars/edit";
        }
        BigDecimal decimalPart = differentAmount.remainder(BigDecimal.ONE);
        int calcPriceOfInt = differentAmount.intValue();
        int calcPriceOfDpf = decimalPart.multiply(new BigDecimal(10)).intValue();

        System.out.println(differentAmount);

        updateCar.setCalcPriceOfInt(calcPriceOfInt);
        updateCar.setCalcPriceOfDpf(calcPriceOfDpf);
        updateCar.setMileage(car.getMileage());

        System.out.println("car : " + updateCar);

        return "redirect:/cars/list";
    }

    // 店名&画像に遷移
    @GetMapping(value = "/shop")
    public String shopRegistration() {
        return "cars/shop_registration";
    }

    // 店名&画像登録処理
    @PostMapping(value = "/shop/submit")
    public String shopRegi(@RequestParam(name = "file", required = false) MultipartFile file, @RequestParam(name = "shopName", required = false) String shopName, HttpSession session, Model model) throws IOException {
        
        if (!file.isEmpty()) {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            session.setAttribute("shopImageBase64", base64Image);
        }
        session.setAttribute("shopName", shopName);

        return "redirect:/cars/list";
    }

    // テンプレート一覧画面表示
    @GetMapping(value = "/template")
    public String templateView(Model model) {
        return "cars/template";
    }

    // テンプレート選択
    @GetMapping(value = "/template/submit")
    public String templateSubmit(@RequestParam(name = "priceCardName", required = false) String priceCardName, HttpSession session) {
        PriceCard priceCard = new PriceCard();
        priceCard.setPriceCardName(priceCardName);
        session.setAttribute("priceCard", priceCard);

        return "redirect:/cars/list";
    }

    // データ取込画面遷移
    @GetMapping(value = "/intake")
    public String intake(Model model) {
        return "cars/intake";
    }

    // データ取込処理
    @PostMapping(value = "/intake/submit")
    public String importData(
        @RequestParam(name = "url", required = false) String url,
        @RequestParam(name = "file", required = false) MultipartFile file,
        Model model, HttpSession session) throws IOException {

        
        List<Car> carList = (List<Car>) session.getAttribute("carList");

        if (carsService.checkGoogleSpreadSheet(url)) {

            List<Car> registerList = carsService.registerGoogleSpreadSheetDataVehicle(url, carList);
            session.setAttribute("carList", registerList);

            System.out.println(registerList);
            return "redirect:/cars/list";
        }
        
        if (carsService.registerExcelFileDataVehicle(file, session)) {
            return "redirect:/cars/list";
        }

        if (carsService.getSpreadSheetId(url) == null || carsService.getSpreadSheetId(url).isEmpty()) {
            System.out.println("getSpreadSheetId");
            return "cars/intake";
        }

        String spreadSheetId = carsService.getSpreadSheetId(url);
        String spreadSheetGid = carsService.getSpreadSheetGid(url);
        try {
            String sheetName = carsService.getSheetNameFromGid(spreadSheetId, spreadSheetGid);

            carsService.getDynamicRange(spreadSheetId, sheetName);
        } catch (GeneralSecurityException e) {

        }
        return "redirect:/cars/list";
    }

    public int toInt(String str) {
        if (str == null) return 0;
        try {
            return Integer.parseInt(str.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
