package eu.depau.etchdroid.utils.worker

interface IProgressForwarder {
    /**
     * Called by the worker service to update the forwarder on the current ACTION index.
     *
     * @param actionIndex the current ACTION index within the JobProcedure
     */
    fun notifyActionStart(actionIndex: Int)

    /**
     * Called by the worker service to notify the forwarder that the current ACTION has finished
     * running. Whether it has finished successfully or not is unspecified.
     *
     * If it failed, a call to notifyProcedureError will follow with an Exception reference.
     */
    fun notifyActionDone()

    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * is starting.
     *
     * @param startActionIndex the ACTION index the procedure is starting with (which is > 0 if it's
     * being bootstrapped from a checkpoint)
     */
    fun notifyProcedureStart(startActionIndex: Int = 0)

    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * has finished successfully.
    */
    fun notifyProcedureDone()

    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * has failed with the provided exception.
     */
    fun notifyProcedureError(error: java.lang.Exception)
}