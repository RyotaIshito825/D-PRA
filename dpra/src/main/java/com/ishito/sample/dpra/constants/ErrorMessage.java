package com.ishito.sample.dpra.constants;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

// エラーメッセージ用クラス
public class ErrorMessage {
    
    private final static Map<ErrorKinds, List<String>> errorMessageMap = new HashMap<ErrorKinds, List<String>>() {

        private static final long serialVersionUID = 1L;

        {
            put(ErrorKinds.BLANK_ERROR, new ArrayList<String>(Arrays.asList("blankError", "値を入力してください")));
            put(ErrorKinds.EMAIL_BLANK_ERROR, new ArrayList<String>(Arrays.asList("emailBlankError", "メールアドレスを入力してください")));
            put(ErrorKinds.EMAIL_DUPLICATION_ERROR, new ArrayList<String>(Arrays.asList("emailDuplicationError", "メールアドレスは既に登録済です")));
            put(ErrorKinds.PASSWORD_BLANK_ERROR, new ArrayList<String>(Arrays.asList("passwordBlankError", "パスワードを入力してください")));
            put(ErrorKinds.PASSWORD_HALFSIZE_ERROR, new ArrayList<String>(Arrays.asList("passwordHalfsizeError", "パスワードは半角英数字のみで入力してください")));
            put(ErrorKinds.PASSWORD_RANGECHECK_ERROR, new ArrayList<String>(Arrays.asList("passwordRangeCheckError", "8文字以上16文字以下で入力してください")));

            put(ErrorKinds.TEXT_BLANK_ERROR, new ArrayList<String>(Arrays.asList("textBlankError",  "名前を入力してください")));
            put(ErrorKinds.TEXT_RANGECHECK_ERROR, new ArrayList<String>(Arrays.asList("textRangeCheckError", "20文字以内で入力してください")));
            put(ErrorKinds.TEXT_20RANGECHECK_ERROR, new ArrayList<String>(Arrays.asList("text20RangecheckError", "20文字以内で入力してください")));
            put(ErrorKinds.THREE_RANGECHECK_ERROR, new ArrayList<String>(Arrays.asList("ThreetRangecheckError", "3文字以内で入力してください")));
            put(ErrorKinds.ONE_RANGECHECK_ERROR, new ArrayList<String>(Arrays.asList("OneRangecheckError", "1文字以内で入力してください")));
            
            put(ErrorKinds.ISNUMBERCHECK_REGISTRATIONYEAR_ERROR, new ArrayList<String>(Arrays.asList("isNumberCheckRegistrationYearError", "初度登録年は数値を入力してください")));

            put(ErrorKinds.GOOGLESPREADSHEET_AUTHORITY_ERROR, new ArrayList<String>(Arrays.asList("googleSpreadSheetAuthorityError", "エラー: アクセス権限が不足しています。スプレッドシートの共有設定や認証情報、APIキーを確認してください")));

            put(ErrorKinds.EMPLOYEE_NOTFOUND_ERROR, new ArrayList<String>(Arrays.asList("employeeNotFoundError", "このメールのユーザーが見つかりません。")));
            
            put(ErrorKinds.FILE_SIZE_ERROR, new ArrayList<String>(Arrays.asList("fileError", "ファイルサイズが大きすぎます。10MB以下にしてください")));
            put(ErrorKinds.FILE_TYPE_ERROR, new ArrayList<String>(Arrays.asList("fileError", "ファイルタイプがエラーです")));

        }
    };

    // エラーメッセージマップにあるエラーかどうかのチェック
    public static boolean contains(ErrorKinds errorKinds) {
        if (errorMessageMap.containsKey(errorKinds)) {
            return true;
        } else {
            return false;
        }
    }

    // エラーメッセージの名称を取得
    public static String getErrorName(ErrorKinds errorKinds) {
        return errorMessageMap.get(errorKinds).get(0);
    }

    // エラーメッセージの値を取得
    public static String getErrorValue(ErrorKinds errorKinds) {
        return errorMessageMap.get(errorKinds).get(1);
    }
}
