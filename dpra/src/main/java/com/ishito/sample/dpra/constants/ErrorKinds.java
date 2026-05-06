package com.ishito.sample.dpra.constants;


// エラーメッセージ定義
public enum ErrorKinds {

    // 空白チェック
    BLANK_ERROR,

    // メール空白チェック
    EMAIL_BLANK_ERROR,

    // メール重複チェック
    EMAIL_DUPLICATION_ERROR,

    // パスワード空白チェック
    PASSWORD_BLANK_ERROR,

    // パスワード半角英数字チェック
    PASSWORD_HALFSIZE_ERROR,

    // パスワード桁数(8桁〜16桁)チェック
    PASSWORD_RANGECHECK_ERROR,

    // 文字空白チェック
    TEXT_BLANK_ERROR,

    // 文字桁数チェック
    TEXT_RANGECHECK_ERROR,

    // 文字桁数(20文字以内)チェック
    TEXT_20RANGECHECK_ERROR,

    // 文字桁数(3文字以内)チェック
    THREE_RANGECHECK_ERROR,

    // 文字桁数(1文字以内)チェック
    ONE_RANGECHECK_ERROR,

    // 数値チェック
    ISNUMBERCHECK_REGISTRATIONYEAR_ERROR,

    // GoogleSpreadシート権限チェック
    GOOGLESPREADSHEET_AUTHORITY_ERROR,

    // ユーザー有無チェック
    EMPLOYEE_NOTFOUND_ERROR,

    // ファイルサイズチェック
    FILE_SIZE_ERROR,

    // ファイルタイプチェック
    FILE_TYPE_ERROR,

    // チェックOK
    CHECK_OK,

    // 正常終了
    SUCCESS;
}
