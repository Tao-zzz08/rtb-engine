package com.example.rtb_engine.mapper;

import com.example.rtb_engine.entity.Ad;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdMapper {

    List<Ad> findCandidates(
            @Param("region") String region,
            @Param("categoryCode") String categoryCode
    );
}