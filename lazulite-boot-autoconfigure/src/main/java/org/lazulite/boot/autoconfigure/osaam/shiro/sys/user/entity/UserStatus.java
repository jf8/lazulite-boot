/*
 * Copyright 2016. junfu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lazulite.boot.autoconfigure.osaam.shiro.sys.user.entity;

public enum UserStatus {

    normal("正常状态"), blocked("封禁状态");

    private final String info;

    private UserStatus(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }
}
