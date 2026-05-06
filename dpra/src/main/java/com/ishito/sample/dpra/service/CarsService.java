package com.ishito.sample.dpra.service;

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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.ishito.sample.dpra.constants.ErrorKinds;
import com.ishito.sample.dpra.entity.Car;
import com.ishito.sample.dpra.entity.Car.Classification;
import com.ishito.sample.dpra.entity.Car.JapaneseCalendar;
import com.ishito.sample.dpra.entity.Car.VehicleInspection;

import com.google.api.client.json.JsonFactory;

import jakarta.servlet.http.HttpSession;

@Service
public class CarsService {
    
    public ErrorKinds createCar(Car car) {
        if (car.getMaker().length() > 20) {
            return ErrorKinds.TEXT_20RANGECHECK_ERROR;
        }
        if (car.getCarModel().length() > 20) {
            return ErrorKinds.TEXT_20RANGECHECK_ERROR;
        }
        if (car.getGrade().length() > 20) {
            return ErrorKinds.TEXT_20RANGECHECK_ERROR;
        }
        return ErrorKinds.SUCCESS;
    }

    /**
     * 削除してリストを返す
     * @param idList
     * @param session
     * @return 削除後のリストを返す
     */
    public List<Car> deleteByCar(List<String> idList, HttpSession session) {

        List<Car> carList = (List<Car>) session.getAttribute("carList");
        for (String id : idList) {

            Car targetCar = carList.stream()
                .filter(car -> car.getId() == Integer.parseInt(id))
                .findFirst()
                .orElse(null);

            carList.remove(targetCar);
        }

        return carList;
    }


    /**
     * carListの最後のIDを取得
     * @param carList
     * @return maxIdもしくは0
     */
    public int getListMaxId(List<Car> carList) {
        int maxId = carList.stream()
            .mapToInt(Car::getId)
            .max()
            .orElse(0);

        return maxId;
    }

    /**
     * Excelファイルからデータを登録する
     * @param file
     * @param session
     * @return 真偽
     * @throws IOException
     */
    public boolean registerExcelFileDataVehicle(MultipartFile file, HttpSession session) throws IOException {

        try {
            byte[] bytes = file.getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

            Workbook wb = WorkbookFactory.create(inputStream);
            Sheet sheet = wb.getSheet("車両一覧");

            List<Car> carList = (List<Car>) session.getAttribute("carList");

            if (carList == null) {
                carList = new ArrayList<>();
                session.setAttribute("carList", carList);
            }

            int lastRowN = sheet.getLastRowNum();
            int actualRowCount = 0;

            // 実際にデータが入っている行を取得
            for (int i = 0; i <= lastRowN; i++) {
                Row r = sheet.getRow(i);
                if (r != null) {
                    boolean isEmpty = true;
                    for (int j = 0; j < r.getPhysicalNumberOfCells(); j++) {
                        Cell cell = r.getCell(j);
                        if (cell != null && cell.getCellType() != CellType.BLANK) {
                            isEmpty = false;
                            break;
                        }
                    }
                    if (!isEmpty) {
                        actualRowCount++;
                    }
                }
            }

            // 見出し行の最終列の取得
            Row headerRow = sheet.getRow(0);
            int headerRowSize = 0;

            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        headerRowSize = i;
                    }
                }
            }

            // carListのidの最大値を取得
            int maxId = getListMaxId(carList);


            for (int i = 2; i < actualRowCount; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Car car = new Car();
                    maxId += 1;

                    // id
                    car.setId(maxId);
                    
                    // メーカー
                    car.setMaker(
                        (row.getCell(7) != null && row.getCell(7).getCellType() == CellType.STRING)
                        ? row.getCell(7).getStringCellValue()
                        : ""
                    );

                    // 車種
                    car.setCarModel(
                        (row.getCell(8) != null && row.getCell(8).getCellType() == CellType.STRING)
                        ? row.getCell(8).getStringCellValue()
                        : ""
                    );

                    // グレード
                    car.setGrade(
                        (row.getCell(9) != null && row.getCell(9).getCellType() == CellType.STRING)
                        ? row.getCell(9).getStringCellValue()
                        : ""
                    );

                    // 新車・未使用車 or 中古車
                    Cell cellClassification = row.getCell(0);
                    String cellClassificationlValue = (cellClassification != null && cellClassification.getCellType() == CellType.STRING) 
                        ? cellClassification.getStringCellValue() 
                        : "";
                    Classification resultClassification = null;
                    for (Classification classification : Car.Classification.values()) {
                        if (classification.getValue().equals(cellClassificationlValue)) {
                            resultClassification = classification;
                            break;
                        };
                    };
                    car.setClassification(resultClassification != null ? resultClassification : Car.Classification.NEW);

                    // 初度登録和暦
                    Cell cellFirstJc = row.getCell(1);
                    String cellFirstJcValue = (cellFirstJc != null && cellFirstJc.getCellType() == CellType.STRING)
                        ? cellFirstJc.getStringCellValue()
                        : "";
                    JapaneseCalendar resultFirstJc = null;
                    for (JapaneseCalendar japaneseCalendar : Car.JapaneseCalendar.values()) {
                        if (japaneseCalendar.getValue().equals(cellFirstJcValue)) {
                            resultFirstJc = japaneseCalendar;
                            break;
                        };
                    };
                    car.setFirstJc(resultFirstJc != null ? resultFirstJc : Car.JapaneseCalendar.REIWA);
                    
                    // 初度登録年
                    car.setRegistrationYear(
                        (row.getCell(2) != null && row.getCell(2).getCellType() == CellType.NUMERIC)
                        ? (int) row.getCell(2).getNumericCellValue()
                        : 0
                    );

                    // 初度登録月
                    car.setRegistrationMonth(
                        (row.getCell(3) != null && row.getCell(3).getCellType() == CellType.NUMERIC)
                        ? (int) row.getCell(3).getNumericCellValue()
                        : 0
                    );

                    // 車検有無
                    Cell cellVehicleInspectionsStatus = row.getCell(5);
                    VehicleInspection cellVehicleInspectionsStatusValue = (cellVehicleInspectionsStatus != null && cellVehicleInspectionsStatus.getCellType() == CellType.NUMERIC)
                        ? Car.VehicleInspection.YES
                        : Car.VehicleInspection.NO;
                    car.setVehicleInspectionStatus(cellVehicleInspectionsStatusValue);

                    // 車検和暦
                    Cell cellViJc = row.getCell(4);
                    String cellViJcValue = (cellViJc != null && cellViJc.getCellType() == CellType.STRING)
                        ? cellViJc.getStringCellValue()
                        : "";
                    JapaneseCalendar resultViJc = null;
                    for (JapaneseCalendar japaneseCalendar : Car.JapaneseCalendar.values()) {
                        if (japaneseCalendar.getValue().equals(cellViJcValue)) {
                            resultViJc = japaneseCalendar;
                            break;
                        }
                    }
                    car.setViJc(resultViJc != null ? resultViJc : Car.JapaneseCalendar.REIWA);

                    // 車検年
                    DataFormatter formatter = new DataFormatter();
                    car.setViYear(
                        (row.getCell(5) != null && row.getCell(5).getCellType() == CellType.NUMERIC)
                        ? formatter.formatCellValue(row.getCell(5))
                        : ""
                    );
                    
                    // 車検月
                    car.setViMonth(
                        (row.getCell(6) != null && row.getCell(6).getCellType() == CellType.NUMERIC)
                        ? formatter.formatCellValue(row.getCell(6))
                        : ""
                    );

                    // プライス
                    car.setPrice(
                        (row.getCell(11) != null && row.getCell(11).getCellType() == CellType.NUMERIC)
                        ? (int) row.getCell(11).getNumericCellValue()
                        : 0
                    );

                    // プライスの小数点
                    car.setPriceDpf(
                        (row.getCell(12) != null && row.getCell(12).getCellType() == CellType.NUMERIC)
                        ? (int) row.getCell(12).getNumericCellValue()
                        : 0
                    );

                    // 総額金額
                    car.setTotalPrice(
                        (row.getCell(13) != null && row.getCell(13).getCellType() == CellType.NUMERIC)
                        ? (int) row.getCell(13).getNumericCellValue()
                        : 0
                    );

                    // 総額金額の小数点
                    car.setTotalPriceDpf(
                        (row.getCell(14) != null && row.getCell(14).getCellType() == CellType.NUMERIC)
                        ? (int) row.getCell(14).getNumericCellValue()
                        : 0
                    );

                    // 諸費用部分
                    BigDecimal differenceAmount = calculateFees(
                        row.getCell(11) != null && row.getCell(11).getCellType() == CellType.NUMERIC ? (int) row.getCell(11).getNumericCellValue() : 0,
                        row.getCell(12) != null && row.getCell(12).getCellType() == CellType.NUMERIC ? (int) row.getCell(12).getNumericCellValue() : 0,
                        row.getCell(13) != null && row.getCell(13).getCellType() == CellType.NUMERIC ? (int) row.getCell(13).getNumericCellValue() : 0,
                        row.getCell(14) != null && row.getCell(14).getCellType() == CellType.NUMERIC ? (int) row.getCell(14).getNumericCellValue() : 0
                    );

                    if (!isTotalPriceHigher(differenceAmount)) {
                        continue;
                    }

                    BigDecimal decimalPart = differenceAmount.remainder(BigDecimal.ONE);
                    int calcPriceOfInt = differenceAmount.intValue();
                    int calcPriceOfDpf = decimalPart.multiply(new BigDecimal(10)).intValue();
                    // 諸費用の整数部分
                    car.setCalcPriceOfInt(calcPriceOfInt);
                    // 諸費用の小数点
                    car.setCalcPriceOfDpf(calcPriceOfDpf);

                    // 走行距離
                    car.setMileage(
                        (row.getCell(10) != null && row.getCell(10).getCellType() == CellType.NUMERIC)
                        ? (int) row.getCell(10).getNumericCellValue()
                        : 0
                    );

                    carList.add(car);
                }
            }

            return true;


        } catch (Exception e) {
            return false;
        }
    }

    /**
     * GoogleスプレッドシートのURLかチェックする
     * @param spreadSheetUrl
     * @return 真偽
     */
    public boolean checkGoogleSpreadSheet(String url) {
        if (url.isBlank() || url == null) {
            return false;
        } else if (url.contains("https://docs.google.com/spreadsheets/")) {
            return true;
        } else {
            return false;
        }
    }



    /**
     * Googleスプレッドシートから車両リストを作成する
     * @param url
     * @param carList
     * @return 車両リストを返す
     */
    public List<Car> registerGoogleSpreadSheetDataVehicle(String url, List<Car> carList) {

        String spreadSheetId = getSpreadSheetId(url);
        String spreadSheetGid = getSpreadSheetGid(url);

        try {
            String sheetName = getSheetNameFromGid(spreadSheetId, spreadSheetGid);
            String dynamicRange = getDynamicRange(spreadSheetId, sheetName);
            List<List<Object>> objects = getGoogleSpreadSheetCarsList(spreadSheetId, dynamicRange);

            // carListのidの最大値を取得
            int maxId = getListMaxId(carList);

            for (List<Object> object: objects) {

                while(object.size() < 15) {
                    object.add("");
                }

                boolean allEmpty = object.stream()
                    .limit(15)
                    .allMatch(v -> v == null || v.equals("") || v.toString().isEmpty());
                if (allEmpty) {
                    continue;
                } else {

                    Car car = new Car();
                    maxId += 1;

                    // id
                    car.setId(maxId);
                    
                    // メーカー
                    car.setMaker(
                        (object.get(7) != null && object.get(7) instanceof String)
                        ? object.get(7).toString()
                        : ""
                    );

                    System.out.println(object.get(7).getClass().getSimpleName());
                    
                    // 車種
                    car.setCarModel(
                        (object.get(8) != null && object.get(8) instanceof String)
                        ? object.get(8).toString()
                        : ""
                    );

                    // グレード
                    car.setGrade(
                        (object.get(9) != null && object.get(9) instanceof String)
                        ? object.get(9).toString()
                        : ""
                    );

                    // 新車・未使用車 or 中古車
                    // Cell cellClassification = (Cell) object.get(0);
                    String cellClassificationlValue = (object.get(0) != null && object.get(0) instanceof String) 
                        ? object.get(0).toString()
                        : "";
                    Classification resultClassification = null;
                    for (Classification classification : Car.Classification.values()) {
                        if (classification.getValue().equals(cellClassificationlValue)) {
                            resultClassification = classification;
                            break;
                        };
                    };
                    car.setClassification(resultClassification != null ? resultClassification : Car.Classification.NEW);

                    

                    // 初度登録和暦
                    // Cell cellFirstJc = (Cell) object.get(1);
                    String cellFirstJcValue = (object.get(1) != null && object.get(1) instanceof String)
                        ? object.get(1).toString()
                        : "";
                    JapaneseCalendar resultFirstJc = null;
                    for (JapaneseCalendar japaneseCalendar : Car.JapaneseCalendar.values()) {
                        if (japaneseCalendar.getValue().equals(cellFirstJcValue)) {
                            resultFirstJc = japaneseCalendar;
                            break;
                        };
                    };
                    car.setFirstJc(resultFirstJc != null ? resultFirstJc : Car.JapaneseCalendar.REIWA);
                    
                    // 初度登録年
                    car.setRegistrationYear(
                        toInt(object.get(2))
                    );

                    // 初度登録月
                    car.setRegistrationMonth(
                        toInt(object.get(3))
                    );

                    // 車検有無
                    // Cell cellVehicleInspectionsStatus = (Cell) object.get(5);
                    VehicleInspection cellVehicleInspectionsStatusValue = (object.get(5) != null && toInt(object.get(5)) != 0)
                        ? Car.VehicleInspection.YES
                        : Car.VehicleInspection.NO;
                    car.setVehicleInspectionStatus(cellVehicleInspectionsStatusValue);

                    // 車検和暦
                    // Cell cellViJc = (Cell) object.get(4);
                    String cellViJcValue = (object.get(4) != null)
                        ? object.get(4).toString()
                        : "";
                    JapaneseCalendar resultViJc = null;
                    for (JapaneseCalendar japaneseCalendar : Car.JapaneseCalendar.values()) {
                        if (japaneseCalendar.getValue().equals(cellViJcValue)) {
                            resultViJc = japaneseCalendar;
                            break;
                        }
                    }
                    car.setViJc(resultViJc != null ? resultViJc : Car.JapaneseCalendar.REIWA);

                    // 車検年
                    DataFormatter formatter = new DataFormatter();
                    car.setViYear(
                        String.valueOf(toInt(object.get(5)))
                    );

                    
                    // 車検月
                    car.setViMonth(
                        String.valueOf(toInt(object.get(6)))
                    );

                    // プライス
                    car.setPrice(
                        toInt(object.get(11))
                    );

                    // プライスの小数点
                    car.setPriceDpf(
                        toInt(object.get(12))
                    );

                    // 総額金額
                    car.setTotalPrice(
                        toInt(object.get(13))
                    );

                    // 総額金額の小数点
                    car.setTotalPriceDpf(
                        toInt(object.get(14))
                    );

                    // 諸費用部分
                    BigDecimal differenceAmount = calculateFees(
                        toInt(object.get(11)),
                        toInt(object.get(12)),
                        toInt(object.get(13)),
                        toInt(object.get(14))
                    );

                    if (!isTotalPriceHigher(differenceAmount)) {
                        continue;
                    }

                    BigDecimal decimalPart = differenceAmount.remainder(BigDecimal.ONE);
                    int calcPriceOfInt = differenceAmount.intValue();
                    int calcPriceOfDpf = decimalPart.multiply(new BigDecimal(10)).intValue();
                    // 諸費用の整数部分
                    car.setCalcPriceOfInt(calcPriceOfInt);
                    // 諸費用の小数点
                    car.setCalcPriceOfDpf(calcPriceOfDpf);

                    // 走行距離
                    car.setMileage(
                        toInt(object.get(10))
                    );

                    carList.add(car);
                }
            }

            return carList;

        } catch (GeneralSecurityException e) {

        } catch (IOException e) {

        } finally {
            return carList;
        }

    }

    /**
     * GoogleスプレッドシートのURLからIDを取得
     * @param spreadSheetUrl
     * @return シートのIDを返す
     */
    public String getSpreadSheetId(String spreadSheetUrl) {

        try {
            // urlの「/d/」と「/edit」の間がIDなので前後のindexを取得して抜き出す
            int index = spreadSheetUrl.indexOf("/d/");
            spreadSheetUrl = spreadSheetUrl.substring(index + 3);
            index = spreadSheetUrl.indexOf("/edit");
            spreadSheetUrl = spreadSheetUrl.substring(0, index);

            return spreadSheetUrl;
            
        } catch (StringIndexOutOfBoundsException e) {
            return  null;
        }
    }

    /**
     * GoogleスプレッドシートのURLからシートGIDを取得
     * @param spreadSheetUrl
     * @return 
     */
    public String getSpreadSheetGid(String spreadSheetUrl) {
        String spreadSheetGid = spreadSheetUrl.substring(spreadSheetUrl.lastIndexOf("gid=") + 4);
        return spreadSheetGid;
    }

    /**
     * Googleスプレッドシートの対象のシート名を取得
     * @param spreadSheetId
     * @param spreadSheetGid
     * @return 
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public String getSheetNameFromGid(String spreadSheetId, String spreadSheetGid) throws IOException, GeneralSecurityException {
        Spreadsheet response = getSpreadsheets().spreadsheets().get(spreadSheetId).execute();
        List<com.google.api.services.sheets.v4.model.Sheet> sheets = response.getSheets();
        if (sheets != null) {
            for (com.google.api.services.sheets.v4.model.Sheet sheet : sheets) {
                String sheetGid = String.valueOf(sheet.getProperties().getSheetId());
                if (sheetGid.equals(spreadSheetGid)) {
                    return sheet.getProperties().getTitle();
                }
            }
        }

        return null;
    }

    /**
     * Sheetsインスタンスの取得
     * @return Googleスプレッドシートのシートを取得
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public Sheets getSpreadsheets() throws IOException, GeneralSecurityException {
        // 環境変数からBase64文字列を取得
        String base64Json = System.getenv("GOOGLE_CREDENTIALS_BASE64");

        if (base64Json == null || base64Json.isEmpty()) {
            throw new IllegalStateException("環境変数 GOOGLE_CREDENTIALS_BASE64 が設定されていません。");
        }

        byte[] decodedJson = Base64.getDecoder().decode(base64Json);
                GoogleCredentials credential = GoogleCredentials.fromStream(new ByteArrayInputStream(decodedJson))
                .createScoped(Arrays.asList(SheetsScopes.SPREADSHEETS));

        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        HttpRequestInitializer httpRequestInitializer = new HttpCredentialsAdapter(credential);

        return new Sheets.Builder(transport, jsonFactory, httpRequestInitializer)
                .setApplicationName("D PRA") // ← 任意のアプリ名でOK
                .build();
    }


    /**
     * Googleスプレッドシートのデータ範囲を取得
     * @param spreadSheetId
     * @param sheetName
     * @return データ範囲を文字列で返す
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public String getDynamicRange(String spreadSheetId, String sheetName) throws IOException, GeneralSecurityException {

        Sheets sheets = getSpreadsheets();
        String range = sheetName + "!A:Z";
        ValueRange valueRange = sheets.spreadsheets().values().get(spreadSheetId, range).execute();
        List<List<Object>> values = valueRange.getValues();
        int lastRowIndex = values.size() - 1;
        String dynamicRange = sheetName + "!A3" + ":O" + (lastRowIndex + 1);

        return dynamicRange;
    }

    /** 
     * 行データをリストとして取得する
     * @param spreadSheetId
     * @param dynamicRange
     * @return 行データをリストで返す
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public List<List<Object>> getGoogleSpreadSheetCarsList(String spreadSheetId, String dynamicRange) throws IOException, GeneralSecurityException {

        Sheets sheets = getSpreadsheets();
        ValueRange valueRange = sheets.spreadsheets().values().get(spreadSheetId, dynamicRange).execute();
        List<List<Object>> values = valueRange.getValues();

        return values;
    }


    /**
     * 諸費用が0以上かどうかチェック
     * @param differenceAmount
     * @return 諸費用価格が0より高い場合はtrue、それ以外はfalse
     */
    public boolean isTotalPriceHigher(BigDecimal differenceAmount) {
        return differenceAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 総額と本体価格から諸費用（差額）を計算する
     * @param price 本体価格の整数部分
     * @param priceDpf 本体価格の小数部分
     * @param totalPrice 総額金額の整数部分
     * @param totalPriceDpf 総額金額の小数部分
     * @return 諸費用金額（総額 - 本体価格）
     */
    public BigDecimal calculateFees(int price, int priceDpf, int totalPrice, int totalPriceDpf) {

        BigDecimal carPrice = new BigDecimal(price);
        BigDecimal carPriceDpf = new BigDecimal(priceDpf).divide(new BigDecimal(10));
        BigDecimal carTotalPrice = new BigDecimal(totalPrice);
        BigDecimal carTotalPriceDpf = new BigDecimal(totalPriceDpf).divide(new BigDecimal(10));
        BigDecimal carPriceAll = carPrice.add(carPriceDpf);
        BigDecimal carTotalPriceAll = carTotalPrice.add(carTotalPriceDpf);

        System.out.println(carTotalPriceAll);

        // 諸費用の計算
        BigDecimal calculateFee = carTotalPriceAll.subtract(carPriceAll);
        return calculateFee;
    }

    /**
     * 文字が数字か否か
     * @param str
     * @return 真偽
     */
    public boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Listをキーワードでフィルタリング
     * @param carList
     * @param keyword
     * @param minPrice
     * @param maxPrice
     * @return キーワードでフィルタリングしたリスト
     */
    public List<Car> filterCarsByKeyword(List<Car> carList, String keyword, Integer minPrice, Integer maxPrice) {
        
        return carList.stream()
            .filter(car -> {
                boolean priceMatch = car.getTotalPrice() != null
                    && car.getTotalPrice() >= minPrice
                    && car.getTotalPrice() <= maxPrice;

                boolean keywordMatch = keyword == null || keyword.isBlank()
                    || containsIgnoreCase(car.getMaker(), keyword)
                    || containsIgnoreCase(car.getCarModel(), keyword)
                    || containsIgnoreCase(car.getGrade(), keyword);

                return priceMatch && keywordMatch;
            })
            .collect((Collectors.toList()));
    }

    /**
     * 大文字小文字を無視して含まれているかチェック
     * @param source
     * @param keyword
     * @return 真偽
     */
    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) return false;

        return source.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * int型数値で返す
     * @param object
     * @return 数値 or 0
     */
    private int toInt(Object object) {
        if (object == null) return 0;
        try {
            return Integer.parseInt(object.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
