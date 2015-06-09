/*
 * Copyright (C) 2011 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.makina.collect.android.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FormDetails
        implements Parcelable {

    public long id;

    @Deprecated
    public String errorStr;

    public String formName;
    public String downloadUrl;
    public String manifestUrl;
    public String formID;
    public String formVersion;
    public String description;
    public String filePath;
    public String directoryPath;
    public boolean checked;

    public FormDetails() {
        checked = false;
    }

    @Deprecated
    public FormDetails(String error) {
        errorStr = error;
        formName = null;
        downloadUrl = null;
        manifestUrl = null;
        formID = null;
        formVersion = null;
        checked = false;
    }

    @Deprecated
    public FormDetails(String name,
                       String url,
                       String manifest,
                       String id,
                       String version) {
        errorStr = null;
        formName = name;
        downloadUrl = url;
        manifestUrl = manifest;
        formID = id;
        formVersion = version;
        checked = false;
    }

    private FormDetails(Parcel source) {
        id = source.readLong();
        errorStr = source.readString();
        formName = source.readString();
        downloadUrl = source.readString();
        manifestUrl = source.readString();
        formID = source.readString();
        formVersion = source.readString();
        description = source.readString();
        filePath = source.readString();
        directoryPath = source.readString();
        checked = (Boolean) source.readValue(null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest,
                              int flags) {
        dest.writeLong(id);
        dest.writeString(errorStr);
        dest.writeString(formName);
        dest.writeString(downloadUrl);
        dest.writeString(manifestUrl);
        dest.writeString(formID);
        dest.writeString(formVersion);
        dest.writeString(description);
        dest.writeString(filePath);
        dest.writeString(directoryPath);
        dest.writeValue(checked);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FormDetails that = (FormDetails) o;

        if (id != that.id) {
            return false;
        }

        if (formName != null ? !formName.equals(that.formName) : that.formName != null) {
            return false;
        }

        if (formID != null ? !formID.equals(that.formID) : that.formID != null) {
            return false;
        }

        return !(formVersion != null ? !formVersion.equals(that.formVersion) : that.formVersion != null);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (formName != null ? formName.hashCode() : 0);
        result = 31 * result + (formID != null ? formID.hashCode() : 0);
        result = 31 * result + (formVersion != null ? formVersion.hashCode() : 0);

        return result;
    }

    public static final Creator<FormDetails> CREATOR = new Creator<FormDetails>() {

        @Override
        public FormDetails createFromParcel(Parcel source) {
            return new FormDetails(source);
        }

        @Override
        public FormDetails[] newArray(int size) {
            return new FormDetails[size];
        }
    };
}
