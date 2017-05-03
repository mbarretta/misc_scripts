@Grab('com.github.groovy-wslite:groovy-wslite:1.1.2')

import wslite.rest.*

final def GITHUB_TOKEN="<GET ONE>"

def client = new RESTClient("https://api.github.com")

//get all the contributors for a given repo and add them to the given list
def doQuery = { repo, list ->
    def done = false
    def path =  "/repos/$repo/contributors"
    while (!done) {
        println "querying $path"
        def contributors = client.get(
            path: path, 
            query: [anon: 1],   //we want anonomous contributors too, which looks to use git account info in liu of github accounts
            headers: [Authorization: "token $GITHUB_TOKEN"],
            sslTrustAllCerts:true
        )
        list.addAll(contributors.json.collect { it.login })     //github accounts do't have e-mails
                                                                //...at least not that are returned from this query
                                                                //perhaps an enhacement is to query the profile endpoint for each login
        list.addAll(contributors.json.collect { it.email })     //anon accounts don't have logins

        //get the link to the next page of results
        if (contributors.response.headers.link.contains("next")) {
            path = ((contributors.response.headers.link =~ /<(.*?)>.*/)[0][1]).substring(22)
        } else {
            done = true
        }
    }
}

def esContributors = [] as HashSet
def sparkContributors = [] as HashSet

//get all the Elastic contributions and smash them together
["elastic/elasticsearch", "elastic/kibana", "elastic/logstash", "elastic/beats"].each { repo->
    doQuery(repo, esContributors)
}

doQuery("apache/spark", sparkContributors)

println "ES: " + esContributors.size()
println "Spark: " + sparkContributors.size()
println "overlap: " + esContributors.intersect(sparkContributors).join("\n")
