@Grab('com.github.groovy-wslite:groovy-wslite:1.1.3')

import groovy.json.*
import wslite.rest.*

//our client
client = new RESTClient("http://localhost:9200/complaints")

//drop existing index
try {
    client.delete()
    println "deleted existing index - let's start fresh!"
} catch (e) {
    //swallow
}

//create the index and mapping
response = client.put() {
    json settings: [
        number_of_shards: 1,
        analysis: [
            filter: [
                shingles_filter: [
                    type: "shingle",
                    min_shingle_size: 2,
                    max_shingle_size: 3,
                    output_unigrams: "false"
                ]
            ],
            analyzer: [
                shingles_analyzer: [
                    type: "custom",
                    tokenizer: "standard",
                    filter: ["lowercase", "shingles_filter"]
                ]
            ]
        ]
    ],
    mappings: [
        complaint: [
            properties: [
                name: [type: "text"],
                text: [
                    type: "text",
                    fields: [
                        shingles: [
                            type: "string",
                            analyzer: "shingles_analyzer"
                        ]
                    ]
                ]
            ]
        ]
    ]
}
if (response.statusCode == 200) {
    println "created index:"
    println JsonOutput.prettyPrint(client.get().json.toString())
}

//load documents
i = 0
sb = new StringBuffer()
new File("/workspace/datasets/mortgage_complaints/raw").eachFile { file ->
    client.post(path: "/complaint") {
        json name: file.name, text: file.text
    }
    i++
}

//validate
client.post(path: "_refresh") //force the buffer to be indexed
println "posted [$i] files"
println "index contains [${client.get(path: "/complaint/_count").json.count}] files"

println client.get(path: "/_search") {
    json query: [
        match: ["text.shingles": "not late"]
    ]
}.json
