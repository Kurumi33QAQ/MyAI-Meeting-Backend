package com.zsj.meetingagent.knowledge.service;

import com.zsj.meetingagent.knowledge.model.JobIntelligenceReport;

/**
 * 岗位情报联网搜索服务。
 * 根据用户实际填写的岗位、公司和 JD 查找公开岗位要求与面试方向。
 */
public interface JobIntelligenceSearchService {

    JobIntelligenceReport search(String jobTitle, String companyName, String jobDescription);
}
