package eu.depau.etchdroid.workers

import android.content.Context
import android.net.Uri
import eu.depau.etchdroid.utils.imagetypes.DMGImage
import java.io.OutputStream


class Dmg2OutputStreamConvertAsyncWorker private constructor(
        private val i2oStreamWorker: Input2OutputStreamCopyAsyncWorker
) : IMergedAsyncWorkerProgressSender by i2oStreamWorker {

    constructor(
            context: Context,
            sourceDmgUri: Uri,
            dest: OutputStream,
            chunkSize: Int
    ) : this(
            DMGImage.getRawImageInputStream(context, sourceDmgUri).let {
                Input2OutputStreamCopyAsyncWorker(it, dest, 0, chunkSize, it.size)
            }
    )
}