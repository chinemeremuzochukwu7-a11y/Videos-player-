package com.example

import com.example.data.VideoMetadata
import com.example.data.VideoMetadataRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun formatDuration_formatsMinutesSecondsAndTenths() {
    assertEquals("00:00", VideoMetadataRepository.formatDuration(0L))
    assertEquals("00:12", VideoMetadataRepository.formatDuration(12_400L))
    assertEquals("00:12.4", VideoMetadataRepository.formatDuration(12_400L, includeTenths = true))
    assertEquals("01:05", VideoMetadataRepository.formatDuration(65_000L))
    assertEquals("1:02:03", VideoMetadataRepository.formatDuration(3_723_000L))
  }

  @Test
  fun formatFileSize_formatsKilobytesMegabytesAndGigabytes() {
    assertEquals("—", VideoMetadataRepository.formatFileSize(0L))
    assertEquals("512 KB", VideoMetadataRepository.formatFileSize(512L * 1024L))
    assertEquals("14.5 MB", VideoMetadataRepository.formatFileSize((14.5 * 1024 * 1024).toLong()))
  }

  @Test
  fun videoMetadata_handlesRotatedAspectRatio() {
    val portraitRotated =
      VideoMetadata(
        uriString = "content://media/video/1",
        displayName = "clip.mp4",
        mimeType = "video/mp4",
        durationMs = 10_000L,
        width = 1920,
        height = 1080,
        fileSizeBytes = 1_000_000L,
        bitrateBps = 8_000_000L,
        rotationDegrees = 90,
      )
    assertEquals(1080f / 1920f, portraitRotated.aspectRatio, 0.01f)
  }

  @Test
  fun dateBucketLabel_classifiesTodayYesterdayAndThisWeek() {
    val now = System.currentTimeMillis()
    val oneDayMs = 24L * 60L * 60L * 1000L
    assertEquals("Today", VideoMetadataRepository.dateBucketLabel(now))
    assertEquals("Yesterday", VideoMetadataRepository.dateBucketLabel(now - oneDayMs))
    assertEquals("This Week", VideoMetadataRepository.dateBucketLabel(now - 3L * oneDayMs))
    assertEquals("Recent", VideoMetadataRepository.dateBucketLabel(0L))
  }

  @Test
  fun formatDateTime_handlesInvalidAndValidTimestamps() {
    assertEquals("", VideoMetadataRepository.formatDateTime(0L))
    val formatted = VideoMetadataRepository.formatDateTime(1_700_000_000_000L)
    assertEquals(true, formatted.isNotBlank())
  }
}
