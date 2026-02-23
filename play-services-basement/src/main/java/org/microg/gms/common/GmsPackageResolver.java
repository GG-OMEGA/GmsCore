/*
 * Copyright (C) 2026 microG Project Team
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

package org.microg.gms.common;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Central package/action resolver for side-by-side GmsCore package handling.
 */
public final class GmsPackageResolver {
    private GmsPackageResolver() {
    }

    public static String[] getCorePackageCandidates(String... preferredPackageCandidates) {
        return dedupeCandidates(
                preferredPackageCandidates,
                Constants.USER_MICROG_PACKAGE_NAME,
                Constants.GMS_PACKAGE_NAME
        );
    }

    public static String[] getActionCandidates(String action) {
        String mappedAction = mapActionAlias(action);
        if (mappedAction == null || mappedAction.equals(action)) {
            return new String[]{action};
        }
        return new String[]{action, mappedAction};
    }

    public static String resolveInstalledPackage(Context context, String... packageCandidates) {
        PackageManager packageManager = context.getPackageManager();
        String[] candidates = getCorePackageCandidates(packageCandidates);
        for (String packageName : candidates) {
            if (packageName == null || packageName.isEmpty()) continue;
            try {
                packageManager.getApplicationInfo(packageName, 0);
                return packageName;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
    }

    public static Context createPackageContext(Context context, int flags, String... packageCandidates) throws PackageManager.NameNotFoundException {
        String[] candidates = getCorePackageCandidates(packageCandidates);
        for (String packageName : candidates) {
            if (packageName == null || packageName.isEmpty()) continue;
            try {
                return context.createPackageContext(packageName, flags);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        throw new PackageManager.NameNotFoundException("No supported GMS package context found");
    }

    public static String resolveServicePackage(Context context, String action, String requiredPermission, String... fallbackPackageCandidates) {
        PackageManager packageManager = context.getPackageManager();
        for (ResolveInfo resolveInfo : packageManager.queryIntentServices(new Intent(action), 0)) {
            if (resolveInfo == null || resolveInfo.serviceInfo == null) continue;
            String packageName = resolveInfo.serviceInfo.packageName;
            if (packageName == null) continue;
            if (requiredPermission == null || packageManager.checkPermission(requiredPermission, packageName) == PERMISSION_GRANTED) {
                return packageName;
            }
        }
        return resolveInstalledPackage(context, fallbackPackageCandidates);
    }

    private static String mapActionAlias(String action) {
        if (action == null) return null;
        String googlePrefix = Constants.GMS_PACKAGE_NAME;
        String repackagedPrefix = Constants.USER_MICROG_PACKAGE_NAME;

        if (action.startsWith(googlePrefix + ".")) {
            return repackagedPrefix + action.substring(googlePrefix.length());
        }
        if (action.startsWith(repackagedPrefix + ".")) {
            return googlePrefix + action.substring(repackagedPrefix.length());
        }
        return null;
    }

    private static String[] dedupeCandidates(String[] packageCandidates, String... appendCandidates) {
        Set<String> candidates = new LinkedHashSet<String>();
        if (packageCandidates != null) {
            for (String candidate : packageCandidates) {
                if (candidate != null && !candidate.isEmpty()) {
                    candidates.add(candidate);
                }
            }
        }
        for (String candidate : appendCandidates) {
            if (candidate != null && !candidate.isEmpty()) {
                candidates.add(candidate);
            }
        }
        return candidates.toArray(new String[0]);
    }
}
