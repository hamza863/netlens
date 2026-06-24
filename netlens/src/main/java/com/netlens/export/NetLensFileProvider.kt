package com.netlens.export

import androidx.core.content.FileProvider

/**
 * Dedicated [FileProvider] subclass for NetLens's HAR export.
 *
 * A library must never register the bare `androidx.core.content.FileProvider`
 * class in its manifest: the manifest merger keys `<provider>` entries by class
 * name, so it would collide with the host app's (or any other library's)
 * FileProvider and fail the build with a duplicate-provider error.
 *
 * Subclassing gives NetLens a unique provider class, so it merges cleanly
 * alongside every other FileProvider. The authority is unchanged.
 */
internal class NetLensFileProvider : FileProvider()
