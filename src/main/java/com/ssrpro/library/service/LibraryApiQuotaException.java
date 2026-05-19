package com.ssrpro.library.service;

/**
 * 정보나루 Open API 일일 호출 한도(미등록 IP 500건/일) 초과.
 */
public class LibraryApiQuotaException extends RuntimeException {

    public static final String USER_MESSAGE =
            "정보나루 API 일일 호출 한도(500건)에 도달했습니다. "
                    + "data4library.kr에서 사용 IP를 등록하거나, 내일 다시 시도해 주세요. "
                    + "이미 조회한 도서는 캐시에서 표시됩니다.";

    public LibraryApiQuotaException(String apiMessage) {
        super(apiMessage);
    }

    public static boolean isQuotaMessage(Object errorNode) {
        if (errorNode == null) {
            return false;
        }
        String msg = String.valueOf(errorNode);
        return msg.contains("500건") || msg.contains("IP 등록");
    }
}
