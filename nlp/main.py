import requests
import spacy
import pickle

pickled_file = "/Users/eginez/src/index-tolken/data/simarill.pkl"
def load_text():
    # load spaCy's language model
    nlp = spacy.load("en_core_web_sm")

    file_location = "/Users/eginez/src/index-tolken/data/silmarill.txt"
    # read the file
    with open(file_location, "r") as file:
        text = file.read()

    # use spaCy to tokenize the text by sentences
    doc = nlp(text)

    with  open(pickled_file, 'wb') as f:
        pickle.dump(doc, f)
    return doc

def load_parsed_text(file):
    with open(file, "rb") as f:
        doc = pickle.load(f)
    return doc

from transformers import pipeline, AutoTokenizer
ner_pipeline = pipeline("ner")
def to_characters(sentence):

    # Run the pipeline on the sentence
    #print(f"ner with {sentence}")
    result = ner_pipeline(sentence)
    def merge_pers(entries):
        merged_pers = []
        for i, entry in enumerate(entries):
            if entry["entity"] == "I-PER":
                if i > 0 and entries[i-1]["entity"] == "I-PER":
                    # If the previous entry was also "I-PER", merge the text
                    entries[i-1]["word"] += entry["word"].replace("##", "")
                else:
                    # If this is the first "I-PER" in a sequence, add it to the list
                    merged_pers.append(entry)
            else:
                # If this is not an "I-PER", add it to the list
                merged_pers.append(entry)
        return merged_pers


    # Extract the characters from the result
    merged_pers_res = merge_pers(result)
    characters = [entry["word"] for entry in merged_pers_res if entry["entity"] in ["PER", "I-PER"]]


    # Output the characters as a JSON object
    return characters
    #print(json.dumps({"characters": characters}))


#doc = load_text()
all_chars = set()
doc = load_parsed_text(pickled_file)
from tqdm.auto import tqdm
sents = list(doc.sents)
for s in tqdm(sents[100:1000]):
    chars = to_characters(s.text)
    all_chars.update(chars)
print(all_chars)

