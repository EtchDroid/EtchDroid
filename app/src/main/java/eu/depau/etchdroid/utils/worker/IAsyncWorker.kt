package eu.depau.etchdroid.utils.worker

interface IAsyncWorker: IProgressSender {
    /**
     * Make the worker run do one step. Returns whether there are more steps to do
     *
     * @return whether more work needs to be done
     */
    fun runStep(): Boolean
}