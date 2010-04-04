package com.bravo.util;

import java.io.Serializable;

public enum SyncServiceError implements Serializable {
    BADCREDENTIALS, CONNECTION, DATASTORE, FILENOTFOUND, NONE, NOTENOUGHSPACE, NOTREGISTERED, UNKNOWN, WEBSERVICE
}
