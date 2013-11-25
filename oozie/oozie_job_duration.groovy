import groovy.json.JsonSlurper
import groovy.time.TimeCategory

import java.util.concurrent.TimeUnit

cli = new CliBuilder(usage: 'groovy job-timings.groovy [options]', header: 'Options:')
cli.oozie(args: 1, argName: 'url', 'oozie url')
cli.job(args: 1, argName: 'job id', 'oozie job id')
cli.consecutiveJobs('assume all ingest jobs follow the first job without interruption and print info for all')
options = cli.parse(args)

duration = { start, end ->
    TimeCategory.minus(new Date(end), new Date(start))
}

formatInfo = { info, indentLevel ->
    "\t" * indentLevel + (indentLevel == 1 ? "id [${info.id}]" : "") + " name [${info.appName ?: info.name}] duration [${duration(info.startTime, info.endTime)}]"
}

printJobInfo = { job, first = false ->
    println "${first ? "first" : "next"} job:\n${formatInfo(job, 1)}"
    println "\tworkflow jobs:"
    job.actions.each {
        println formatInfo(it, 2)
    }
}

getJobInfo = { id ->
    new JsonSlurper().parseText("${options.oozie}/v1/job/$id?show=info".toURL().text)
}

formatDuration = { millis ->
    return String.format("%02d hours, %02d minutes, %02d seconds",
        TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))
}

if (options && options.oozie && options.job) {
    def info = getJobInfo(options.job)

    //capture times for overall duration println
    def jobDuration = duration(info.startTime, info.endTime).toMilliseconds()

    //do it
    printJobInfo(info, true)
    if (options.consecutiveJobs) {
        def idParts = options.job.split("-")

        //total ingest has 10 worflows (as of 3.7.0-m4)
        10.times {
            //increment the job id: "0000099-xyz" -> "0000100-xyz"
            idParts[0] = "${++(idParts[0] as int)}".padLeft(idParts[0].length(), "0")

            def subinfo = getJobInfo(idParts.join("-"))
            printJobInfo(subinfo)

            //add job duration to overall
            jobDuration += duration(subinfo.startTime, subinfo.endTime).toMilliseconds()
        }
    }

    //do it for all subsequent jobs in the ingest, if so told
    println "OVERALL DURATION: " + formatDuration(jobDuration)
} else {
    cli.usage()
}
