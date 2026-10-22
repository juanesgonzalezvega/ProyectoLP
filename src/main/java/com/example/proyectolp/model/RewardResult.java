package com.example.proyectolp.model;
import java.util.List;
public record RewardResult(int credits,int xp,List<String> badges,String message){public RewardResult{badges=List.copyOf(badges);}}
