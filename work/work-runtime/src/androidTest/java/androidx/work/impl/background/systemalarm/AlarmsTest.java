/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.work.impl.background.systemalarm;

import static androidx.work.impl.model.SystemIdInfoKt.systemIdInfo;
import static androidx.work.impl.model.WorkSpecKt.generationalId;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.work.DatabaseTest;
import androidx.work.OneTimeWorkRequest;
import androidx.work.impl.model.SystemIdInfo;
import androidx.work.impl.model.WorkGenerationalId;
import androidx.work.worker.TestWorker;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AlarmsTest extends DatabaseTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    // Set it to sometime in the future so as to avoid triggering real alarms.
    private final long mTriggerAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void testSetAlarm_noPreExistingAlarms() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);
        WorkGenerationalId workSpecId = generationalId(work.getWorkSpec());

        Alarms.setAlarm(mContext, mDatabase, workSpecId, mTriggerAt);
        SystemIdInfo systemIdInfo = mDatabase.systemIdInfoDao().getSystemIdInfo(workSpecId);
        assertThat(systemIdInfo, is(notNullValue()));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void testSetAlarm_withPreExistingAlarms() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);
        WorkGenerationalId workSpecId = generationalId(work.getWorkSpec());

        SystemIdInfo systemIdInfo = systemIdInfo(workSpecId, 1);
        mDatabase.systemIdInfoDao().insertSystemIdInfo(systemIdInfo);

        Alarms.setAlarm(mContext, mDatabase, workSpecId, mTriggerAt);
        SystemIdInfo updatedSystemIdInfo = mDatabase.systemIdInfoDao().getSystemIdInfo(workSpecId);
        assertThat(updatedSystemIdInfo, is(notNullValue()));
        assertThat(updatedSystemIdInfo.systemId, is(systemIdInfo.systemId));
    }

    @Test
    public void testCancelAlarm() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);
        WorkGenerationalId workSpecId = generationalId(work.getWorkSpec());

        SystemIdInfo systemIdInfo = systemIdInfo(workSpecId, 1);
        mDatabase.systemIdInfoDao().insertSystemIdInfo(systemIdInfo);

        Alarms.cancelAlarm(mContext, mDatabase, workSpecId);
        SystemIdInfo updatedSystemIdInfo = mDatabase.systemIdInfoDao().getSystemIdInfo(workSpecId);
        assertThat(updatedSystemIdInfo, is(nullValue()));
    }
}
