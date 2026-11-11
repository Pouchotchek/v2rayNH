package com.v2ray.ang.handler

import com.v2ray.ang.dto.GitHubRelease
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UpdateCheckerManagerTest {
    private fun release(vararg names: String) = GitHubRelease(
        tagName = "v1.1.0",
        body = "",
        assets = names.map { GitHubRelease.Asset(it, it) },
    )

    @Test
    fun selectsAnApkForTheDeviceAndDistribution() {
        val arm = "v2rayNH_1.1.0_arm64-v8a.apk"
        val x86 = "v2rayNH_1.1.0-fdroid_x86.apk"
        val release = release("$arm.sig", x86, arm, "v2rayNG_2.3.15_arm64-v8a.apk")
        assertEquals(arm, UpdateCheckerManager.selectReleaseApk(release, listOf("arm64-v8a"), false))
        assertEquals(x86, UpdateCheckerManager.selectReleaseApk(release, listOf("x86_64", "x86"), true))
        assertNull(UpdateCheckerManager.selectReleaseApk(release, listOf("x86"), false))
    }

    @Test
    fun fallsBackToTheUniversalApkFromTheSameDistribution() {
        val universal = "v2rayNH_1.1.0_universal.apk"
        val release = release(universal)
        assertEquals(universal, UpdateCheckerManager.selectReleaseApk(release, listOf("x86_64"), false))
        assertNull(UpdateCheckerManager.selectReleaseApk(release, listOf("x86_64"), true))
    }
}
