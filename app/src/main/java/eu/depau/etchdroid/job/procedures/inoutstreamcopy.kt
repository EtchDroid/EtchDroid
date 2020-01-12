package eu.depau.etchdroid.job.procedures

import android.content.Context
import eu.depau.etchdroid.R
import eu.depau.etchdroid.job.JobProcedureBuilder
import eu.depau.etchdroid.job.actions.Input2OutputStreamCopyAction
import eu.depau.etchdroid.job.actions.StreamCloseAction
import eu.depau.etchdroid.job.actions.StreamOpenAction
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.IJobProcedure
import eu.depau.etchdroid.utils.job.enums.SharedDataType
import eu.depau.etchdroid.utils.job.impl.JobProcedure
import eu.depau.etchdroid.workers.enums.StreamDirection

/**
 * Procedure for a simple dd-like copy
 */

fun JobProcedureBuilder.buildInOutStreamCopyProcedure(context: Context): IJobProcedure {
    val inputName = autoName(inputType!!, input!!, context)
    val outputName = autoName(outputType!!, output!!, context)
    val procedureName = StringResBuilder(R.string.write_x_to_y, inputName, outputName)
    val procedure = JobProcedure(procedureName)

    return procedure.apply {
        // Open streams
        add(
                StreamOpenAction(
                        name = StringResBuilder(R.string.empty_string, "Open InputStream for '$inputName'"),
                        progressWeight = 0.0,
                        checkpoint = null,
                        destKey = SharedDataType.INPUT_STREAM,
                        streamDirection = StreamDirection.INPUT,
                        targetDescriptor = input!!,
                        targetType = inputType!!,
                        showInGUI = false,
                        runAlways = true
                )
        )

        add(
                StreamOpenAction(
                        name = StringResBuilder(R.string.empty_string, "Open OutputStream for '$outputName'"),
                        progressWeight = 0.0,
                        checkpoint = null,
                        destKey = SharedDataType.OUTPUT_STREAM,
                        streamDirection = StreamDirection.OUTPUT,
                        targetDescriptor = output!!,
                        targetType = outputType!!,
                        showInGUI = false,
                        runAlways = true
                )
        )

        // Pipe data from input to output
        add(
                Input2OutputStreamCopyAction(
                        name = StringResBuilder(R.string.i2o_action_name, inputName, outputName),
                        progressWeight = 100.0,
                        checkpoint = null /* TODO: implement */,
                        showInGUI = true,
                        runAlways = false
                )
        )

        // Close streams
        add(
                StreamCloseAction(
                        name = StringResBuilder(R.string.empty_string, "Close InputStream for '$inputName'"),
                        progressWeight = 0.0,
                        checkpoint = null,
                        destKey = SharedDataType.INPUT_STREAM,
                        showInGUI = false,
                        runAlways = true
                )
        )
        add(
                StreamCloseAction(
                        name = StringResBuilder(R.string.empty_string, "Close OutputStream for '$outputName'"),
                        progressWeight = 0.0,
                        checkpoint = null,
                        destKey = SharedDataType.OUTPUT_STREAM,
                        showInGUI = false,
                        runAlways = true
                )
        )
    }
}