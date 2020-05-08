import os


def get_project_list():

    examples_folder = "C:\\Users\\maxim\\Documents\\Results\\Summer2018\\exampleRepos"
    filenames = os.listdir(examples_folder)  # get all files' and folders' names in the current directory

    result = []
    for filename in filenames:  # loop through all the files and folders
        if os.path.isdir(
                os.path.join(os.path.abspath(examples_folder), filename)):  # check whether the current object is a folder or not
            result.append(os.path.join(os.path.abspath(examples_folder), filename))

    return result


for git_repo in get_project_list():
    print(git_repo)

