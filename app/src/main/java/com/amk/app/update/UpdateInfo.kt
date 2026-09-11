package com.amk.app.update

data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long
)

data class GitHubRelease(
    val tag_name: String,
    val name: String?,
    val body: String?,
    val prerelease: Boolean,
    val draft: Boolean,
    val published_at: String?,
    val html_url: String,
    val assets: List<GitHubAsset>
)

data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkUrl: String?,
    val apkName: String?,
    val apkSizeMb: String?,
    val isNightly: Boolean
)
