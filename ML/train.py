import torch
import torch.nn as nn
import torch.optim as optim
from torchvision import datasets, transforms
from torch.utils.data import DataLoader
import os
from model import CarConditionModel

# Config
BATCH_SIZE = 16
EPOCHS = 5
LR = 0.001
DATA_DIR = "dataset"

# Transforms
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
])

# Dataset
train_dataset = datasets.ImageFolder(os.path.join(DATA_DIR, "train"), transform=transform)
val_dataset = datasets.ImageFolder(os.path.join(DATA_DIR, "val"), transform=transform)

train_loader = DataLoader(train_dataset, batch_size=BATCH_SIZE, shuffle=True)
val_loader = DataLoader(val_dataset, batch_size=BATCH_SIZE, shuffle=False)

# Model
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = CarConditionModel().to(device)

criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(model.parameters(), lr=LR)

# Training loop
for epoch in range(EPOCHS):
    model.train()
    total_loss = 0
    for imgs, labels in train_loader:
        imgs, labels = imgs.to(device), labels.to(device)

        # map: clean=0, dirty=1, intact=2, damaged=3
        cleanliness_labels = (labels == 1).long()  # 0 clean, 1 dirty
        damage_labels = (labels == 3).long()       # 0 intact, 1 damaged

        optimizer.zero_grad()
        clean_pred, damage_pred = model(imgs)
        loss = criterion(clean_pred, cleanliness_labels) + criterion(damage_pred, damage_labels)
        loss.backward()
        optimizer.step()
        total_loss += loss.item()

    print(f"Epoch {epoch+1}/{EPOCHS}, Loss: {total_loss/len(train_loader):.4f}")

torch.save(model.state_dict(), "car_condition.pth")
print("✅ Training finished! Model saved to car_condition.pth")