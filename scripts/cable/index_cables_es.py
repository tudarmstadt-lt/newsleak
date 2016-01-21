import argparse
import codecs
import traceback
import random
from random import randint
from random import random
from math import ceil
import requests
from datetime import date
import random
from os.path import join
from time import sleep
import cStringIO as StringIO

HEADERS = {'content-type': 'application/json'}
LOCATIONS = ["Darmstadt", "Louvain", "Grenoble", "Moscow", "Brussels", "Berlin", "Paris", "Ghent"]
CABLES_TYPE = "cables"
VERBOSE = False
SCHEMA = """
{
    "cables": {
        "properties": {
            "title": {
                "type":     "string",
                "analyzer": "english"
            },  
            "date": {
                "type": "date"
            },
            "score" : {
                "type": "integer"
            },
            "location": {
                "type": "string"
            }
        }
    }
}"""
TEST_QUERY = """
{
    "query": {
        "match": {
            "title": "blanket"
        }
    }
}"""


def cln(text):
    return text.replace('"',' ').replace("'"," ").replace("{"," ").replace("}"," ").replace(":", " ").replace("\n"," ")


def random_date():
    start_date = date.today().replace(day=1, month=1, year=1985).toordinal()
    end_date = date.today().toordinal()
    random_day = date.fromordinal(random.randint(start_date, end_date))
    return unicode(random_day)


def create_index(endpoint):
    response = requests.post(endpoint, data="", headers=HEADERS)
    print "create index:", response.status_code
    if VERBOSE: print response.text    

    print join(join(endpoint, "_mapping"), CABLES_TYPE)
    response = requests.post(join(join(endpoint, "_mapping"), CABLES_TYPE), data=SCHEMA, headers=HEADERS)
    print "create schema:", response.status_code
    if VERBOSE: print response.text    


def test_query(endpoint):
    print join(join(endpoint, CABLES_TYPE), "_search")
    response = requests.post(join(join(endpoint, CABLES_TYPE), "_search"), data=TEST_QUERY, headers=HEADERS)
    print "test_query:", response.status_code
    print response.text    


def index_cables(cables_fpath, endpoint, chunk_size):
    with codecs.open(cables_fpath, "r", "utf-8") as sentences:
        chunk = StringIO.StringIO()

        for i, line in enumerate(sentences):
            try:
                tid, text = line.split("\t")
                text = cln(text)
                score = randint(0, 100)
                datef = random_date()
                location = LOCATIONS[randint(0, len(LOCATIONS)-1)]

                es_command = u'{ "index": { "_id": %d }}\n' % int(tid)
                es_data = u'{ "title": "%s", "location": "%s", "date":"%s", "score": "%d" }\n' % (text, location, datef, score)
                chunk.write(es_command.encode('utf-8'))
                chunk.write(es_data.encode('utf-8'))

                if i > 0 and i % chunk_size == 0:
                    chunk_str = chunk.getvalue()
                    response = requests.post(join(endpoint, join(CABLES_TYPE,"_bulk")), data=chunk_str, headers=HEADERS)
                    print "indexed %d texts: %d" % (i, response.status_code)
                    
                    chunk.close()
                    chunk = StringIO.StringIO()
            except:
                print "Errr:", line.encode("utf-8")
                print traceback.format_exc()
        
        chunk.close()

def main():
    parser = argparse.ArgumentParser(description="Index cables to ElasticSearch.")
    parser.add_argument('cables', help='Path to an input file with cables in the format "id<TAB>text".')
    parser.add_argument('endpoint', help='ElasticSearch endpoint index where index will be created e.g. http://localhost:9200/leaks')
    parser.add_argument('--chunk_size', help='Number of text that will be inserted in one bulk query. Default -- 10000.', default="10000")
    parser.add_argument('--verbose', action='store_true', help='Verbose output.')
    args = parser.parse_args()

    print "Cables: ", args.cables
    print "Endpoint: ", args.endpoint
    print "Chunk size:", args.chunk_size
    print "Verbose:", args.verbose

    global VERBOSE
    VERBOSE = args.verbose
    create_index(args.endpoint)
    index_cables(cables_fpath=args.cables,
                 endpoint=args.endpoint,
                 chunk_size=int(args.chunk_size))
    sleep(5)  # wait while index is ready
    test_query(args.endpoint)

if __name__ == '__main__':
    main()        
