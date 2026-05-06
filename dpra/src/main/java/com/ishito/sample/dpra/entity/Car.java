package com.ishito.sample.dpra.entity;


import com.ishito.sample.dpra.entity.Car.Classification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
@Entity
@Table(name = "cars")
public class Car {
    
    public static enum Classification {
        NEW("新車・未使用車"), USED("中古車");

        private String name;
        private Classification(String name) {
            this.name = name;
        }
        public String getValue() {
            return this.name;
        }
    }

    public static enum JapaneseCalendar {
        REIWA("R"), HEISEI("H");

        private String name;
        private JapaneseCalendar(String name) {
            this.name = name;
        }
        public String getValue() {
            return this.name;
        }
    }

    public static enum VehicleInspection {
        YES("有"), NO("無");

        private String name;
        private VehicleInspection(String name) {
            this.name = name;
        }
        public String getValue() {
            return this.name;
        }
    }

    // ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // IDを自動生成する設定
    private Integer id;
    
    // メーカー
    @NotBlank(message = "メーカーを入力してください")
    // @NotNull(message = "メーカーを入力してください")
    @Size(max = 20, message = "メーカーは20文字以内で入力してください")
    private String maker;

    // 車種
    @NotBlank(message = "車種を入力してください")
    @NotNull(message = "車種を入力してください")
    @Size(max = 20, message = "車種は20文字以内で入力してください")
    @Column(nullable = false) // null禁止
    private String carModel;

    // グレード
    @NotBlank(message = "グレードを入力してください")
    @Size(max = 20, message = "グレードは20文字以内で入力してください")
    @Column(nullable = false)
    private String grade;

    // 区分 enumの新車・未使用車 or 中古車
    @Column(columnDefinition="VARCHAR(10)", nullable = false)
    @Enumerated(EnumType.STRING) // enumをDBにString型で保存するアノ
    private Classification classification;

    // 初度登録和暦
    @Column(columnDefinition="VARCHAR(10)", nullable = false)
    @Enumerated(EnumType.STRING)
    private JapaneseCalendar firstJc;

    // 初度登録年
    @NotNull(message = "初度登録年を入力してください")
    // @Digits(integer = 2, fraction = 0, message = "入力値は整数で桁数は2桁までを入力してください")
    @Column(columnDefinition="VARCHAR(10)", nullable = false)
    @Min(value = 1, message = "初度登録年は1〜99の間で入力してください")
    @Max(value = 99, message = "初度登録年は1〜99の間で入力してください")
    // @Enumerated(EnumType.STRING)
    private Integer registrationYear;

    // 初度登録月
    @NotNull(message = "初度登録月を入力してください")
    @Column(length = 2, nullable = false)
    @Min(value = 1, message = "初度登録月は1〜12の間で入力してください")
    @Max(value = 12, message = "初度登録月は1〜12の間で入力してください")
    private Integer registrationMonth;

    // 車検有無
    @NotNull(message = "車検有無を選択してください")
    @Enumerated(EnumType.STRING)
    private VehicleInspection vehicleInspectionStatus;

    // 車検和暦
    @Column(columnDefinition="VARCHAR(10)")
    // @Enumerated(EnumType.STRING)
    private JapaneseCalendar viJc;

    // 車検年
    @Column(length = 2)
    // @Pattern(regexp = "^$|^(1[0-2]|[1-9])$", message = "車検月は1〜12の間で入力してください")
    private String viYear;

    // 車検月
    @Column(length = 2)
    // @Pattern(regexp = "^$|^(1[0-2]|[1-9])$", message = "車検月は1〜12の間で入力してください")
    private String viMonth;

    // プライス
    @NotNull(message = "プライスを入力してください")
    @Max(value = 9999, message = "プライスが異常な桁数です。正しく入力してください")
    @Column(length = 4, nullable = false)
    private Integer price;

    // プライスの小数点
    @NotNull(message = "プライスの小数点第一を入力してください")
    @Max(value = 9, message = "プライスの小数点第一の桁は1桁のみ入力してください")
    @Column(length = 1, nullable = false)
    private Integer priceDpf;

    // 総額金額
    @NotNull(message = "総額を入力してください")
    @Max(value = 9999, message = "総額が異常な桁数です。正しく入力してください")
    @Column(length = 4, nullable = false)
    private Integer totalPrice;

    // 総額金額の小数点
    @NotNull(message = "総額の小数点第一を入力してください")
    @Max(value = 9, message = "総額の小数点第一の桁は1桁のみ入力してください")
    @Column(length = 1, nullable = false)
    private Integer totalPriceDpf;

    // 諸費用金額
    // @NotNull(message = "諸費用の小数点第一を入力してください")
    // @Max(value = 9, message = "諸費用の小数点第一の桁は1桁のみ入力してください")
    // @Column(length = 4)
    private Integer calcPriceOfInt;

    // 初期用金額の小数点
    // @NotNull(message = "諸費用の小数点第一を入力してください")
    // @Max(value = 1, message = "諸費用の小数点第一の桁は1桁のみ入力してください")
    // @Column(length = 1)
    private Integer calcPriceOfDpf;

    // 走行距離
    @NotNull(message = "走行距離を入力してください")
    @Max(value = 999999, message = "走行距離の桁数が多すぎます。正しく入力してください")
    @Column(length = 6, nullable = false)
    private Integer mileage;
}
