package bn.client.api.exceptions;

public enum WebserviceError {
    ACCOUNT_EXISTS, ACCOUNT_ISSUE__1127, ACCOUNT_ISSUE__1127_taskservice, ACCOUNT_ISSUE__235_digitalproducts,

    ACCOUNT_ISSUE__diglib_100,

    ACCOUNT_LOCKED,

    ALREADY_ADDED_SAMPLE,

    ALREADY_PURCHASED,

    BILLING_ADDRESS_IS_INTERNATIONAL,

    CHUNK_LENGTH_TOO_LARGE,

    CHUNK_NOT_IN_RANGE,

    CREDIT_CARD_EXPIRED,

    CREDIT_CARD_HASH_ISSUE,

    DEVICE_ALREADY_REGISTERED,

    DEVICE_BLACKLISTED_CALL_CUSTOMER_SERVICE,

    DEVICE_REQUIRES_KEY_REGENERATION,

    DEVICE_REQUIRES_REGISTRATION,

    DEVICE_UNAUTHORIZED,

    DUPLICATE_BORROWER_BORROWED,

    DUPLICATE_BORROWER_PURCHASED,

    DUPLICATE_ITEM_EXISTS,

    EAN_NOT_STREAMABLE,

    EMAIL_NOT_ON_RECORD,

    EPUB_NOT_AVAILABLE,

    EPUB_PROCESSING_FAILED,

    FAILED_ADDRESS_VALIDATION,

    FEATURE_ACCESS_SUSPENDED,

    FORMAT_ERROR,

    FRAUD_DETECTED,

    INPUT_VALIDATION_ERROR,

    INVALID_API_VERSION,

    INVALID_CHUNK_LENGTH,

    INVALID_CITY_LENGTH,

    INVALID_CREDIT_CARD_NUMBER_FORMAT,

    INVALID_CREDIT_CARD_NUMBER_FOR_TYPE,

    INVALID_CURATED_CONTENT_ID,

    INVALID_DELIVERY_ITEM,

    INVALID_DELIVERY_REQUEST_FORMAT,

    INVALID_EAN,

    INVALID_EMAIL_FORMAT,

    INVALID_EVENT,

    INVALID_EXPIRATION_DATE,

    INVALID_FEED_URL,

    INVALID_FILENAME,

    INVALID_FIRST_NAME_LENGTH,

    INVALID_LAST_NAME_LENGTH,

    INVALID_LEND_INPUT,

    INVALID_PASSWORD,

    INVALID_PASSWORD_LAST_CHANCE,

    INVALID_PASSWORD_LENGTH,

    INVALID_PHONE_NUMBER_FORMAT,

    INVALID_PHONE_NUMBER_LENGTH,

    INVALID_POSTAL_CODE_FORMAT,

    INVALID_SECURITY_CODE,

    INVALID_SECURITY_QUESTION_ANSWER_LENGTH,

    INVALID_SECURITY_QUESTION_ID,

    INVALID_SERIAL_NUMBER,

    INVALID_SERIAL_NUMBER_FORMAT,

    INVALID_STORE,

    ITEM_ALREADY_ON_LIST,

    ITEM_EXCEEDS_QUANTITY_LIMIT,

    ITEM_NOT_ON_LIST,

    LEND_OFFER_EXPIRED,

    LIST_LIMIT_REACHED,

    MAX_LOANS_REACHED,

    MISSING_ADDRESS_PRIMARY_LINE,

    MISSING_FILE_OFFSET,

    MISSING_POSTAL_CODE,

    MISSING_PUBLIC_KEY,

    MISSING_SERIAL_NUMBER,

    MISSING_STATE,

    NEXT_FAILURE_WILL_LOCK_ACCOUNT,

    NOT_AUTHENTICATED,

    NOT_AVAILABLE_FOR_LOAN,

    NOT_AVAILABLE_FOR_PURCHASE,

    NOT_PENDING,

    NOT_REGISTERED,

    NO_DEFAULT_BILLING_ADDRESS,

    NO_DEFAULT_CREDIT_CARD,

    NO_SYNC_AUTHENTICATION_RESPONSE,

    OFFSET_TOO_LARGE,

    PAYMENT_ISSUE_CALL_CUSTOMER_SERVICE,

    PREORDER,

    PUBLIC_PRIVATE_KEY_MISMATCH,

    RECIPIENT_EMAIL_BLOCKED,

    SERVER_ERROR,

    SERVICE_BUSY_TRY_AGAIN,

    TITLE_BLOCKED,

    UNEXPECTED,

    UNLOCK_CREDIT_CARD_INVALID_OR_EXPIRED,

    UNLOCK_CREDIT_CARD_MISSING_BILLING_ADDRESS
    
}