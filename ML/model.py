import torch.nn as nn
from torchvision import models

class CarConditionModel(nn.Module):
    def __init__(self):
        super(CarConditionModel, self).__init__()
        self.backbone = models.resnet18(weights=models.ResNet18_Weights.DEFAULT)
        in_features = self.backbone.fc.in_features
        self.backbone.fc = nn.Identity()

        # two heads
        self.cleanliness_head = nn.Linear(in_features, 2)  # clean/dirty
        self.damage_head = nn.Linear(in_features, 2)       # intact/damaged

    def forward(self, x):
        features = self.backbone(x)
        return self.cleanliness_head(features), self.damage_head(features)