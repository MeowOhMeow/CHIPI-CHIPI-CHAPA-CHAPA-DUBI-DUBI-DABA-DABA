import onnxruntime as ort 
import cv2
import numpy as np
# import sys
# print(sys.path)

# 載入yolo模型
sess = ort.InferenceSession('best.onnx')


img = cv2.imread("45fb2e43-0564-41c0-8fb9-51f92c226a2e.png")
image = img.copy()
cv2.imshow("original image", image)
original_shape = img.shape[:2] #保持原始圖片形狀 #img.shape[0]是高度、img.shape[1]是寬度
print("original_shape:", original_shape)
# img = cv2.resize(img, (640, 640)) 
# cv2.imshow("Square image", img)
# cv2.waitKey(0)

#調整圖片大小
_max = max(img.shape[0], img.shape[1])
if( _max == img.shape[0]):
    new_height = 640
    ratio = new_height / img.shape[0]
    new_width = int(img.shape[1]*ratio)
elif( _max == img.shape[1]):
    new_width = 640
    ratio = new_width / img.shape[1]
    new_height = int(img.shape[0]*ratio)
# define new dimension
new_dim = (new_width, new_height)
# show new dimension
print('Dimensions of the new image will be: \n height: {}'' \n width: {}'
       .format(new_dim[1], new_dim[0]))
# resize the image according to new dimensions
resized = cv2.resize(image, new_dim, interpolation=cv2.INTER_AREA)
# display resized image
cv2.imshow("Resized Image", resized)
cv2.waitKey(0)

#轉換成可輸入模型形式
# 创建一个空白的640x640矩阵
blank_image = np.zeros((640, 640, 3), np.uint8) #像素初始值0，黑色
# 计算将调整大小后的图像放入空白矩阵中的位置
start_x = (640 - resized.shape[1]) // 2
start_y = (640 - resized.shape[0]) // 2
end_x = start_x + resized.shape[1]
end_y = start_y + resized.shape[0]
# 将调整大小后的图像放入空白矩阵中
blank_image[start_y:end_y, start_x:end_x] = resized
# 显示结果图像
cv2.imshow("Resized and Placed Image", blank_image)
cv2.waitKey(0)


blank_image = blank_image.transpose(2, 0, 1) #channel*height*widht
blank_image = blank_image.astype(np.float32) / 255
blank_image = np.expand_dims(blank_image, axis=0) #num*channel*height*widht
print("input_shape:", blank_image.shape)

#法一----------------------------------------------------------------------------------------------
#獲得輸出結果
input_name = sess.get_inputs()[0].name
output_name = sess.get_outputs()[0].name
pred = sess.run([output_name], {input_name: blank_image})
output0 = np.squeeze(pred[0]).transpose()
print("output_shape:", output0.shape)

print("input_name:", input_name)
print("output_name:", output_name)
print("result: ", pred)

rows = output0.shape[0]
boxes = []
classIds = []
scores = []
index = []
for i in range(rows):
    # Extract the class scores from the current row
    classes_scores = output0[i][4:]

    # Find the maximum score among the class scores
    max_score = np.amax(classes_scores)

    # If the maximum score is above the confidence threshold
    if max_score >= 0.5:
        # Get the class ID with the highest score
        class_id = np.argmax(classes_scores)

        # Extract the bounding box coordinates from the current row
        x, y, w, h = output0[i][0], output0[i][1], output0[i][2], output0[i][3]

        # Calculate the scaled coordinates of the bounding box
        left = int((x - w / 2))
        top = int((y - h / 2))
        width = int(w)
        height = int(h)

        # Add the class ID, score, and box coordinates to the respective lists
        index.append(i)
        classIds.append(class_id)
        scores.append(max_score)
        boxes.append([left, top, width, height])
        
# 非极大值抑制
indices = cv2.dnn.NMSBoxes(boxes, scores, 0.5, 0.4)
 # Iterate over the selected indices after non-maximum suppression
for i in indices:
    # Get the box, score, and class ID corresponding to the index
    index_num = index[i]
    box = boxes[i]
    score = scores[i]
    class_id = classIds[i]

    # Draw the detection on the input image
    
    print("index_num", index_num)
    print("box:", box)
    print("score:", score)
    print("class_id:", class_id)

from collections import Counter

#---------------------------------------------------------------------------------------------------------

#法二-----------------------------------------------------------------------------------------------------
# #獲得輸出結果
# input_name = sess.get_inputs()[0].name
# output_name = sess.get_outputs()[0].name
# output = sess.run([output_name], {input_name: blank_image})[0]
# # 获取模型的输出信息列表
# outputs_info = sess.get_outputs()
# # 遍历输出信息列表，打印每个输出的名称和形状
# for output_info in outputs_info:
#     print("Output Name:", output_info.name)
#     print("Output Shape:", output_info.shape)
# # 解析輸出
# output_shape = output.shape
# print(output_shape)
# reshaped_output = output.reshape(output_shape[0], output_shape[1], output_shape[2])
# print(reshaped_output.shape) #一樣
# #提取訊息
# boxes = reshaped_output[..., 0:4]
# print("boxes_shape", boxes.shape) #(1, 14, 4)
# print("boxes:\n", boxes) 
# print("\n")

# objectness = reshaped_output[..., 4]
# print("objectness_shape", objectness.shape) #(1, 14)
# print("objectness:\n", objectness)
# print("\n")

# class_probs = reshaped_output[..., 5: ] #(1, 14, 8395)
# print("class_probs_shape:", class_probs.shape)
# print("class_probs:\n", class_probs)
    
# # 閥值設定
# threshold = 0.5
# iou_num = 0.5
# detection_area_list = []
# # 畫框
# for box, obj, class_prob in zip(boxes.reshape(-1, 4), objectness.reshape(-1), class_probs.reshape(-1, class_probs.shape[-1])): #[-1]代表最後一個元素
#     if(obj > threshold):
#         x, y, w, h = box
#         # x = int((x-start_x)*img.shape[1]/640)
#         # y = int((y-start_y)*img.shape[0]/640)
#         # w = int(w*original_shape[1]/640)
#         # h = int(h*original_shape[0]/640)
        
#         class_id = np.argmax(class_prob)
#         confidence = obj
#         detection_area_list.append([class_id, confidence, x, y, w, h])

# for detection in detection_area_list:
#     print(detection)
    
# # 在原图上绘制边界框
# for detection in detection_area_list:
#     class_id, confidence, x, y, w, h = detection
#     color = (0, 255, 0)  # 绿色边界框
    
#     # 将检测到的对象的位置和尺寸转换为整数
#     x, y, w, h = int(x), int(y), int(w), int(h)
    
#     cv2.rectangle(resized, (x, y), (x + w, y + h), color, 2)
#     cv2.putText(resized, f'Class: {class_id}, Conf: {confidence:.2f}', (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)

# # 显示图像
# cv2.imshow("Result", resized)
# cv2.waitKey(0)
#-----------------------------------------------------------------------------------------------------------



# # 輸出模型訊息
# import onnx
# model = onnx.load(r"best.onnx")

# # The model is represented as a protobuf structure and it can be accessed
# # using the standard python-for-protobuf methods

# # iterate through inputs of the graph
# for input in model.graph.input:
#     print (input.name, end=": ")
#     # get type of input tensor
#     tensor_type = input.type.tensor_type
#     # check if it has a shape:
#     if (tensor_type.HasField("shape")):
#         # iterate through dimensions of the shape:
#         for d in tensor_type.shape.dim:
#             # the dimension may have a definite (integer) value or a symbolic identifier or neither:
#             if (d.HasField("dim_value")):
#                 print (d.dim_value, end=", ")  # known dimension
#             elif (d.HasField("dim_param")):
#                 print (d.dim_param, end=", ")  # unknown dimension with symbolic name
#             else:
#                 print ("?", end=", ")  # unknown dimension with no name
#     else:
#         print ("unknown rank", end="")
#     print()