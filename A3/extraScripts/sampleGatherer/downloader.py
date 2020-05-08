import requests
import os
import time
from git import Repo


def main():

    outputDirectory = os.path.abspath("C:/Users\maxim\Documents\Results\Summer2018\ICSE_PROJECT\exampleRepos")

    desiredOrganization = "orgs/googlesamples"

	#Enter a github token here, must get one of your own
    token=""

    for pageNumber in range(1, 10):
        http = "https://{}/{}/repos?access_token={}&page={}".format("api.github.com", desiredOrganization, token, pageNumber)
        print("Looking at {}".format(http))
        request = requests.get(http)

        for item in request.json():
            projectName = cleanString(item["name"])

            newDir = os.path.join(outputDirectory, projectName)
            try:
                print("Making directory for {}".format(projectName))
                os.mkdir(newDir)
                time.sleep(2)
                print("Downloading {}".format(projectName))
                Repo.clone_from(item["clone_url"], newDir)
            except FileExistsError as e:
                print("Directory {} already exists: skipping".format(projectName))


def cleanString(string):
    bannedCharacters = ['/', '!', '*', '.', '<', '>', '?', '[', ']', '|', '{', '}', '-', '+', '=', '#', '%', '^', '&', '(', ')', '~']

    for character in bannedCharacters:
        if character in string:
            string = string.replace(character, "")

    return string


if __name__ == "__main__":
    main()