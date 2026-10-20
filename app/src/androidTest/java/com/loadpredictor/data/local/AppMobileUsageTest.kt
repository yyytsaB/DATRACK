package com.loadpredictor.data.local
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loadpredictor.data.stats.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class AppMobileUsageTest {
 @Test fun realMobileUidsHaveValidShares()=runBlocking {
  val c=ApplicationProvider.getApplicationContext<Context>();check(c.packageName.endsWith(".audit"))
  assumeTrue(UsageAccessHelper(c).hasUsageAccessPermission())
  val end=System.currentTimeMillis();val start=end-14*86400000L
  val report=AppMobileUsageDataSource(c).query(start,end)
  assertEquals(start,report.start);assertEquals(end,report.end)
  assertEquals(report.apps.size,report.apps.map {it.uid}.distinct().size)
  report.apps.forEach {assertTrue(it.bytes>0);assertTrue(it.label.isNotBlank());assertTrue(report.share(it) in 0f..1f)}
  val manager=c.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
  @Suppress("DEPRECATION") val device=manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,null,start,end)
  if(device.rxBytes+device.txBytes>0) assertTrue("Android must expose per-UID mobile records",report.apps.isNotEmpty())
  println("UID groups=${report.apps.size}, attributed bytes=${report.totalBytes}")
 }
}
