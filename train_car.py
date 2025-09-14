from roboflow import Roboflow
from ultralytics import YOLO

rf = Roboflow(api_key="2mFtBoxmzGTDKCMV9qzA")

datasets = [
    "seva-at1qy/rust-and-scrach",
    "carpro/car-scratch-and-dent",
    "project-kmnth/car-scratch-xgxzs"
]

for ds in datasets:
    # use full workspace/project slug
    project = rf.project(ds)
    dataset = project.version(1).download("yolov8")
    print(f"Downloaded {ds} to {dataset.location}")

    model = YOLO("yolov8n.pt")
    model.train(
        data=f"{dataset.location}/data.yaml",
        epochs=50,
        imgsz=640,
        batch=16,
        project="CarScratchTraining",
        name=ds.split("/")[1]
    )