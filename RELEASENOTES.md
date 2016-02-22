<!--
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
-->
# Release Notes

### 2.1.0 (Feb 22, 2016)
* Fix bug with undefined watchId
* Position.timestamp is now a Number

Thanks @chemerisuk and @flexarts

### 2.0.1 (Dec 12, 2015)
* Fix broken watch/clear Position
* Remove Google Play Services version constraint

Thanks @cvaliere

### 2.0.0 (Nov 21, 2015)
* Add support for runtime permissions on Android >= 6
* Remove objects from global, use cordova.plugins.locationServices.*

### 1.2.0 (Nov 12, 2015)
* Update Google Play Services to v7.8

Thanks @mlegenhausen!

### 1.1.0 (Aug 07, 2015)
* Move `LocationServices` object to `cordova.plugins.locationServices.geolocation`
* `window.LocationServices` is still available
* Update tests

### 1.0.2 (Jul 26, 2015)
* Fix License and copyright header.

### 1.0.1 (Jul 26, 2015)
* Update README and package.json for npm web page.

### 1.0.0 (Jul 26, 2015)
* Change id from `fr.louisbl.cordova.locationservices` to `cordova-plugin-locationservices`
* Publish to npm

### 0.2.2 (Jul 26, 2015)
* Fix crash when dismissing Google Play Services error dialog
* Add google play services automatically to the project

Many thanks to @mlegenhausen.

### 0.2.1 (Oct 22, 2014)
* Fix crash by removing usage of cordova getThreadPool

### 0.2.0 (Oct 20, 2014)
* Use GoogleApiClient instead of the deprecated LocationClient
* Rename plugin and feature to LocationServices

### 0.1.0 (Sept 30, 2014)
* First version
