package com.example.proyectolp.service;
import com.example.proyectolp.repository.XpLogRepository;
public class XpService {private final XpLogRepository repo;public XpService(SupabaseService db){repo=new XpLogRepository(db);}public boolean grant(int amount,String reason,String key)throws Exception{return amount>0&&repo.add(amount,reason,key);}public int total()throws Exception{return repo.total();}}
