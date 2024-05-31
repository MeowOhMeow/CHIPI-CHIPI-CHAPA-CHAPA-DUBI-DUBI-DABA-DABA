import cv2
from cv2 import dnn_superres

def getQualityValues(upsampled, orig):
    psnr = cv2.PSNR(upsampled, orig)
    q, _ = cv2.quality.QualitySSIM_compute(upsampled, orig)
    ssim = (q[0] + q[1] + q[2]) / 3
    return round(psnr, 3), round(ssim, 3)

def main():
    # Set the main thread as the GUI thread
    cv2.setNumThreads(0)
    cv2.ocl.setUseOpenCL(False)
       
    algorithm = ""
    path = ""
    #選擇算法和給予模型路徑
    option = 2
    scale = 2
    if option == 1:
        algorithm = "edsr"
        path = "./models/EDSR_x2.pb"
    elif option == 2:
        algorithm = "espcn"
        path = "./models/ESPCN_x2.pb"
    elif option == 3:
        algorithm = "fsrcnn"
        path = "./models/FSRCNN_x4.pb"
    elif option == 4: 
        algorithm = "lapsrn"
        path = "./models/LapSRN_x4.pb"
    # 放大比例，可输入值2，3，4    
    
    img_path = "Astronaut_result.png"
    img = cv2.imread(img_path)
    if img is None:
        print("Couldn't load image: " + str(img_path))
        return    
    
    # width = img.shape[0] - (img.shape[0] % scale)
    # height = img.shape[1] - (img.shape[1] % scale)
    # cropped = img[0:width, 0:height]
    # img_downscaled = cv2.resize(cropped, None, fx=1.0 / scale, fy=1.0 / scale)
    
    # sharpen
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    sharp = cv2.GaussianBlur(gray, (0, 0), 3)
    sharp = cv2.addWeighted(gray, 1.5, sharp, -0.5, 0)
    sharp = cv2.cvtColor(sharp, cv2.COLOR_GRAY2BGR)
    
    # 创建模型
    sr = dnn_superres.DnnSuperResImpl_create()
    if algorithm == "bilinear":
        img_new = cv2.resize(img, None, fx=scale, fy=scale, interpolation=cv2.INTER_LINEAR)
    elif algorithm == "bicubic":
        img_new = cv2.resize(img, None, fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC)
    elif algorithm == "edsr" or algorithm == "espcn" or algorithm == "fsrcnn" or algorithm == "lapsrn":
        sr.readModel(path)
        sr.setModel(algorithm, scale)
        # img_new = sr.upsample(img)
        # img_new = sr.upsample(img_downscaled)
        img_new = sr.upsample(sharp)
    else:
        print("Algorithm not recognized")
    # If failed
    if img_new is None:
        print("Upsampling failed")
    print("Upsampling succeeded. \n")
    
    # psnr, ssim = getQualityValues(img_new, cropped)
    # psnr, ssim = getQualityValues(img_new, img)
    # print(sr.getAlgorithm() + ", " + "scale: " + str(scale) + "\n")
    # print("PSNR: " + str(psnr) + " SSIM: " + str(ssim) + "\n")
    
    # img_new = cv2.resize(img_new, None, fx=1.0 / scale, fy=1.0 / scale)
    
    # Display
    cv2.namedWindow("Initial Image", cv2.WINDOW_AUTOSIZE)
    # 初始化图片
    cv2.imshow("Initial Image", img)
    cv2.imshow("Altered Image", img_new)
    # string = algorithm + "_x" + str(scale) + ".jpg"
    string = algorithm + "_x" + str(scale) + "+sharpend" + ".jpg"
    cv2.imwrite(string, img_new)
    cv2.waitKey(0)
    
    print(img.shape)
    print(img_new.shape)
if __name__ == '__main__':
    main()
