/*
 * Copyright (C) 2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.network.authentication.litres;

import org.geometerplus.fbreader.network.NetworkErrors;
import org.geometerplus.zlibrary.core.xml.ZLStringMap;


class LitResRegisterUserXMLReader extends LitResAuthenticationXMLReader {

    private static final String TAG_AUTHORIZATION_OK = "catalit-authorization-ok";
    private static final String TAG_REGISTRATION_FAILED = "catalit-registration-failed";

    public String Sid;

    public LitResRegisterUserXMLReader(String hostName) {
        super(hostName);
    }

    @Override
    public boolean startElementHandler(String tag, ZLStringMap attributes) {
        tag = tag.toLowerCase().intern();
        if (TAG_REGISTRATION_FAILED == tag) {
            final String error = attributes.getValue("error");
            if ("1".equals(error)) {
                setErrorCode(NetworkErrors.ERROR_LOGIN_ALREADY_TAKEN);
            } else if ("2".equals(error)) {
                setErrorCode(NetworkErrors.ERROR_LOGIN_WAS_NOT_SPECIFIED);
            } else if ("3".equals(error)) {
                setErrorCode(NetworkErrors.ERROR_PASSWORD_WAS_NOT_SPECIFIED);
            } else if ("4".equals(error)) {
                setErrorCode(NetworkErrors.ERROR_INVALID_EMAIL);
            } else if ("5".equals(error)) {
                setErrorCode(NetworkErrors.ERROR_TOO_MANY_REGISTRATIONS);
            } else {
                setErrorCode(NetworkErrors.ERROR_INTERNAL);
            }
        } else if (TAG_AUTHORIZATION_OK == tag) {
            Sid = attributes.getValue("sid");
        } else {
            setErrorCode(NetworkErrors.ERROR_SOMETHING_WRONG, HostName);
        }
        return true;
    }
}
