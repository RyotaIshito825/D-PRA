package com.ishito.sample.dpra.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ishito.sample.dpra.entity.Car;
import com.ishito.sample.dpra.entity.PriceCard;
import com.ishito.sample.dpra.service.CarsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class PdfController {

    private final CarsService carService;

    @Autowired
    public PdfController(CarsService carService) {
        this.carService = carService;
    }
    

    @GetMapping(value = "/generate-pdf")
    public String generatePdf(
        HttpServletRequest req,
        @RequestParam(name = "priceCardName", required = false) String priceCardName,
        @RequestParam(name = "keyword", required = false) String keyword,
        @ModelAttribute Car oneCar, RedirectAttributes redirectAttributes,
        Model model,
        HttpSession session) {


        if (priceCardName == null) {

            model.addAttribute("keyword", session.getAttribute("keyword"));
            model.addAttribute("carList", session.getAttribute("carList"));
            
            redirectAttributes.addFlashAttribute("message", "テンプレートを選択してください");
            return "redirect:/cars/list";
        }

        // 支払月の設定
        int currentMonth = LocalDate.now().getMonthValue(); // 1-12

        String priceCardNum = priceCardName.replace("template_image-", "");

        PriceCard priceCard = (PriceCard) req.getSession().getAttribute("priceCard");

        String shopName = (String) session.getAttribute("shopName");
        String shopImageBase64 = (String) session.getAttribute("shopImageBase64");

        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("shopName", shopName);
        model.addAttribute("shopImageBase64", shopImageBase64);

        List<Car> carList = new ArrayList<>();
        Car car = oneCar;
        carList.add(car);

        model.addAttribute("carList", carList);

        model.addAttribute("calcPriceOfInt", car.getCalcPriceOfInt());
        model.addAttribute("calcPriceOfDpf", car.getCalcPriceOfDpf());

        if (car.getClassification().getValue().equals("新車・未使用車")) {
            model.addAttribute("with", false);
            model.addAttribute("none", true);
        } else {
            model.addAttribute("with", true);
            model.addAttribute("none", false);
        }

        return "pricecards/pricecard" + priceCardNum;
    }


    @GetMapping(value = "generate-pdfs")
    public String generatePdfLists(
        @RequestParam(name = "priceCardName", required = false) String priceCardName,
        @RequestParam(name = "id", required = false) List<String> id,
        @RequestParam(name = "option", required = false) String option,
        RedirectAttributes redirectAttributes,
        Model model,
        HttpServletRequest req,
        HttpSession session
    ) {

        if (option.isBlank()) {
            System.out.println("blank");
        } else if (option.isEmpty()) {
            System.out.println("empty");
        } else if (option == "") {
            System.out.println("空欄");
        }
        System.out.println("option : " + option);
        

        if (option.equals("一括削除") || option.equals("delete")) {
            if (id != null) {
                List<Car> carList = carService.deleteByCar(id, session);
                session.setAttribute("carList", carList);
                model.addAttribute("carList", carList);
            } else {
                redirectAttributes.addFlashAttribute("message", "削除する車両を選択してください");
            }
            return "redirect:/cars/list";
        } else if (option.isBlank()) {
            redirectAttributes.addFlashAttribute("message", "処理を選択してください");
            return "redirect:/cars/list";
        }

        if (priceCardName == null) {
            redirectAttributes.addFlashAttribute("message", "テンプレートを選択してください");
            return "redirect:/cars/list";
        }
        if (id == null) {
            redirectAttributes.addFlashAttribute("message", "一括作成する車両を選択してください");
            return "redirect:/cars/list";
        }

        List<Car> carList = (List<Car>) session.getAttribute("carList");

        // プライスカードの番号取得
        String priceCardNum = priceCardName.replace("template_image-", "");
        // 支払月の設定
        int currentMonth = LocalDate.now().getMonthValue();
        // 店舗名の取得
        String shopName = (String) session.getAttribute("shopName");
        // 画像の取得
        String shopImageBase64 = (String) session.getAttribute("shopImageBase64");

        model.addAttribute("shopName", shopName);
        model.addAttribute("shopImageBase64", shopImageBase64);
        model.addAttribute("carList", carList);
        model.addAttribute("currentMonth", currentMonth);

        return "pricecards/pricecard" + priceCardNum;
    }
}
