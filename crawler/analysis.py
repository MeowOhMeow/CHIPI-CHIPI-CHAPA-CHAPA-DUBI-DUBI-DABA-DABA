import csv
import numpy as np
import matplotlib.pyplot as plt

#檔案名稱要依序叫做result_1.csv、result_2.csv......以此類推
#預設整理數據到organized.csv再去作其它分析

def calculate_area_averages(file_name, variable_of_y_axis):
    area_scores = {0: [], 1: [], 2: [], 3: []}  

    with open(file_name, 'r', newline='') as csvfile:
        reader = csv.reader(csvfile)
        for row in reader:
            if row:  
                total_score, game_time, time_score, area_index = map(float, row)
                area_index = int(area_index)
                if area_index in area_scores:
                    if(variable_of_y_axis == "total score"):
                        area_scores[area_index].append(total_score)
                    elif(variable_of_y_axis == "time score"):
                        area_scores[area_index].append(time_score)

    area_average = {}
    for area_index, scores in area_scores.items():
        if scores:
            area_average[area_index] = sum(scores) / len(scores)
        else:
            area_average[area_index] = 0  
        print(f"Area {area_index}: {area_average[area_index]}")

    return area_average

def plot_area_averages(area_average):
    indices = list(area_average.keys())
    averages = list(area_average.values())
    
    plt.scatter(indices, averages, color='blue', s=50)  # s为点的大小
    
    plt.xlabel('Area Index')
    plt.ylabel('Average Total Score')
    plt.title('Average Total Score by Area Index')
    
    # Optional: Add value labels on each point
    for i, txt in enumerate(averages):
        plt.annotate(round(txt, 2), (indices[i], averages[i]), textcoords="offset points", xytext=(0,10), ha='center')

    plt.show()
    
def find_max_and_min(variable):
    area_scores = {0: [], 1: [], 2: [], 3: []}  

    with open(main_file, 'r', newline='') as csvfile:
        reader = csv.reader(csvfile)
        for row in reader:
            if row:  
                total_score, game_time, time_score, area_index = map(float, row)
                area_index = int(area_index)
                if area_index in area_scores:
                    if(variable == "total score"):
                        area_scores[area_index].append(total_score)
                    elif(variable == "time score"):
                        area_scores[area_index].append(time_score)

    area_max = {}
    area_min = {}
    for area_index, scores in area_scores.items():
        if scores:
            area_max[area_index] = np.max(scores)
            area_min[area_index] = np.min(scores)
        else:
            area_max[area_index] = 0  
        print(f"Area {area_index}, max:{area_max[area_index]}, min:{area_min[area_index]}")
        
def plot_distribution(file_name, variable_of_y_axis):
    indices = []
    scores = []
    
    with open(file_name, 'r', newline='') as csvfile:
        reader = csv.reader(csvfile)
        for row in reader:
            if row:  
                total_score, game_time, time_score, area_index = map(float, row)
                area_index = int(area_index)
                if(variable_of_y_axis == "total score"):
                    indices.append(area_index)
                    scores.append(total_score)  
                elif(variable_of_y_axis == "time score"):
                    indices.append(area_index)
                    scores.append(time_score)           
                              
        
        plt.scatter(indices, scores, color='blue', s=50)  # s为点的大小
        
        plt.xlabel('Area Index')
        plt.ylabel(f"{variable_of_y_axis}")
        plt.title(f"Distribution of {variable_of_y_axis}")
        plt.xticks(range(min(indices), max(indices)+1))
        
        # Optional: Add value labels on each point
        for i, txt in enumerate(scores):
            plt.annotate(round(txt, 2), (indices[i], scores[i]), textcoords="offset points", xytext=(0,10), ha='center')

        plt.show()


print("Which function do you want to excute?(Input number)")
print("1. Integrate all data in this directory to \"organized.csv\".\n2. Add a row.\n3. Delete a row.\n4. Display distributions of all areas.\n5. Calculate average.\n6. Find max and min.")
main_file = 'organized.csv'
instruction = int(input())

if(instruction == 1):
    num = int(input("Number of files:"))
    with open(main_file, 'w', newline='') as csvfile_1:
        w = csv.writer(csvfile_1)

        for idx in range(1, num+1):
            filename = f'result_{idx}.csv' 
            with open(filename, 'r', newline='') as csvfile_2:  # 設定 newline=''

                r = csv.reader(csvfile_2)
                next(r)  # 跳過第一行
                for row in r:  # 直接遍歷 reader
                    w.writerow(row)
                
    print("Data has been written to organized.csv.")
    
elif(instruction == 2):
    with open(main_file, 'a', newline='') as csvfile:
        r = csv.writer(csvfile)
        num = int(input("Number of rows?"))
        i = 0
        print("Enter total_score, game_time, time_score and area_index:")
        
        while(i < num):
            user_input = input()
            total_score, game_time, time_score, area_index = user_input.split()
            r.writerow([total_score, game_time, time_score, area_index])
            i+=1
    print(f"Data added to {main_file}.")
    
elif(instruction == 3):
    row_to_delete = int(input("Enter the row number to delete: "))
    with open(main_file, 'r', newline='') as csvfile:
        reader = csv.reader(csvfile)
        rows = list(reader)
    rows.pop(row_to_delete-1)
    
    with open(main_file, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerows(rows)
    print(f"Row {row_to_delete} has been deleted from {main_file}.")
    
elif(instruction == 4):
    variable = input("Choose variable to analyze.(Input \"total score\" or \"time score\")\n")
    plot_distribution(main_file, variable)
    
elif(instruction == 5):
    #calcute average and plot
    variable = input("Choose variable to analyze.(Input \"total score\" or \"time score\")\n")
    area_average = calculate_area_averages(main_file, variable)
    plot_area_averages(area_average)
    
elif(instruction == 6):
    #find maximum
    variable = input("Choose variable to analyze.(Input \"total score\" or \"time score\")\n")
    find_max_and_min(variable)
    
        