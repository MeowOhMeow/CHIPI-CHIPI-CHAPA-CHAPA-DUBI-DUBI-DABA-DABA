import cv2

def getQualityValues(upsampled, orig):
    psnr = cv2.PSNR(upsampled, orig)
    q, _ = cv2.quality.QualitySSIM_compute(upsampled, orig)
    ssim = (q[0] + q[1] + q[2]) / 3
    return round(psnr, 3), round(ssim, 3)

img_path = "Astronaut_result.png"
img = cv2.imread(img_path)

# scale = 2
# width = img.shape[1] - (img.shape[1] % scale)
# height = img.shape[0] - (img.shape[0] % scale)
# cropped = img[0:width, 0:height]
# img_downscaled = cv2.resize(cropped, None, fx=1.0 / scale, fy=1.0 / scale)

# dst = cv2.laplacian = cv2.Laplacian(img_downscaled, cv2.CV_64F, scale, ksize = 3)
# abs_dst = cv2.convertScaleAbs(dst)
# abs_dst_resized = cv2.resize(abs_dst, (cropped.shape[1], cropped.shape[0]))
# inverted_image = cv2.bitwise_not(abs_dst_resized)

# psnr, ssim = getQualityValues(cropped, inverted_image)
# print("PSNR: " + str(psnr) + " SSIM: " + str(ssim) + "\n")


gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
sharp = cv2.GaussianBlur(gray, (0, 0), 3)
sharp = cv2.addWeighted(gray, 1.5, sharp, -0.5, 0)
sharp = cv2.cvtColor(sharp, cv2.COLOR_GRAY2BGR)


cv2.imshow("Initial Image", img)
cv2.imshow("shapened Image", sharp)
print(sharp.shape)
cv2.waitKey(0)
cv2.imwrite("shapened.jpg", sharp)
