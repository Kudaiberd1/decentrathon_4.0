import torch
import torchvision.transforms as transforms
from PIL import Image
from ultralytics import YOLO
from model import CarConditionModel   # your classifier

# -----------------------------
# Load YOLO model (object detection)
# -----------------------------
yolo_model = YOLO("CarScratchTraining/car-scratch-and-dent4/weights/best.pt")

# -----------------------------
# Load classifier model (Clean/Dirty, Intact/Damaged)
# -----------------------------
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
])

clf_model = CarConditionModel()
clf_model.load_state_dict(torch.load("car_condition.pth", map_location="cpu"))
clf_model.eval()

# -----------------------------
# Run inference on one image
# -----------------------------
def run_inference(image_path, conf=0.25):
    # ---- 1. YOLO detections ----
    yolo_results = yolo_model.predict(image_path, save=True, conf=conf)
    detections = []
    for r in yolo_results:
        for box in r.boxes:
            cls_id = int(box.cls)
            label = yolo_model.names[cls_id]
            confidence = float(box.conf)
            xyxy = box.xyxy.tolist()[0]
            detections.append({"label": label, "confidence": confidence, "box": xyxy})
    print(detections)
    # ---- 2. Classification (Clean/Dirty, Intact/Damaged) ----
    img = Image.open(image_path).convert("RGB")
    img_t = transform(img).unsqueeze(0)

    with torch.no_grad():
        clean_pred, damage_pred = clf_model(img_t)

    # raw scores
    clean_scores = torch.softmax(clean_pred, dim=1).cpu().numpy()[0]
    damage_scores = torch.softmax(damage_pred, dim=1).cpu().numpy()[0]

    clean_label = torch.argmax(clean_pred, dim=1).item()
    damage_label = torch.argmax(damage_pred, dim=1).item()

    # NOTE: check your dataset labels, maybe swap these if wrong
    cleanliness = "Dirty" if clean_label != 0 else "Clean"
    try:
        condition  = "Damaged" if detections[0]['confidence'] != 0 else "Intact"
    except:
        condition = 'Intact'

    # ---- 3. Output ----
    return {
        "detections": detections,
        "car_status": {
            "cleanliness": cleanliness,
            "condition": condition,
            "clean_scores": clean_scores.tolist(),
            "damage_scores": damage_scores.tolist()
        },
        "result_image_dir": yolo_results[0].save_dir
    }


if __name__ == "__main__":
    test_img = "image.png"  # put your car image here
    result = run_inference(test_img)
    print("YOLO detections:", result["detections"])
    print("Car Status:", result["car_status"])
    print("✅ Annotated image saved in:", result["result_image_dir"])