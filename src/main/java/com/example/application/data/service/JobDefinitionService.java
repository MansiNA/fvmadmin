package com.example.application.data.service;

import com.example.application.data.entity.JobManager;
import com.example.application.data.repository.JobDefinitionRepository;
import com.example.application.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobDefinitionService {

    private final JobDefinitionRepository jobDefinitionRepository;
    private List<JobManager> jobManagerList;
    private Map<Integer, JobManager> jobManagerMap;
    @Autowired
    public JobDefinitionService(JobDefinitionRepository jobDefinitionRepository) {
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.jobManagerList = jobDefinitionRepository.findAll();
        this.jobManagerMap = buildJobManagerMap(jobManagerList);
    }

    private Map<Integer, JobManager> buildJobManagerMap(List<JobManager> jobManagerList) {
        return jobManagerList.stream()
                .collect(Collectors.toMap(JobManager::getId, jobManager -> jobManager));
    }

    public List<JobManager> findAll() {
        jobManagerList = jobDefinitionRepository.findAll();
        jobManagerMap = buildJobManagerMap(jobManagerList);  // Rebuild the map
        return jobManagerList;
    }

    public JobManager getJobManagerById(Integer id) {
        JobManager jobManager = jobDefinitionRepository.findById(id).get();
        return jobManager;
    }

    public JobManager save(JobManager jobManager) {
        return jobDefinitionRepository.save(jobManager);
    }

    public void deleteById(Integer id) {
        jobDefinitionRepository.deleteById(id);
    }

    public List<JobManager> getRootJobManager() {
        System.out.println("-----------"+ jobManagerList.size()+"-----------------------------");
        List<JobManager> rootProjects = jobManagerList
                .stream()
                .filter(sqlDef -> sqlDef.getPid() == 0)
                .collect(Collectors.toList());

        // Log the names of root projects
        rootProjects.forEach(project -> System.out.println("Root Project: " + project.toString()));

        return rootProjects;
    }

    public List<JobManager> getChildJobManager(JobManager parent) {

        List<JobManager> childProjects = jobManagerList
                .stream()
                .filter(sqlDef -> Objects.equals(sqlDef.getPid(), parent.getId()))
                .collect(Collectors.toList());

        // Log the names of child projects
        childProjects.forEach(project -> System.out.println("Child Project of " + parent.getName() + ": " + project.getName()));

        return childProjects;
    }

    public List<JobManager> getJobchainList(JobManager parent) {
        List<JobManager> jobchain = new ArrayList<>();
        gatherChildJobsRecursively(parent, jobchain);
        return jobchain;
    }

    // Helper method to gather child jobs recursively
    private void gatherChildJobsRecursively(JobManager parent, List<JobManager> jobchain) {
        List<JobManager> childJobs = getChildJobManager(parent); // Reuse the existing method to get direct children

        // Add the direct child jobs to the jobchain list
        jobchain.addAll(childJobs);

        // Log child job information
        childJobs.forEach(child -> System.out.println("+++++++++++++++++++++++Child of " + parent.getName() + ": " + child.getName()));

        // Recursively gather children of each child job
        for (JobManager childJob : childJobs) {
            gatherChildJobsRecursively(childJob, jobchain);
        }
    }


    private boolean hasAccess(String projectRoles) {
        if (projectRoles != null) {
            String[] roleList = projectRoles.split(",");
            // here noted rolelist have ADMIN, USER like that
            // here noted userRoles have ROLE_ADMIN, ROLE_USER like that
            for (String role : roleList) {
                for (String userRole : MainLayout.userRoles) {
                    if (userRole.contains(role)){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public Map<Integer, JobManager> getJobManagerMap() {
        return jobManagerMap;
    }

    public List<String> getUniqueTypList() {
//        List<String> uniqueTyps = jobManagerList.stream()
//                .map(JobManager::getTyp)
//                .distinct()               // Ensure the values are unique
//                .collect(Collectors.toList());  // Collect into a List

        List<String> uniqueTyps = new ArrayList<>();
        uniqueTyps.add("sql_procedure");
        uniqueTyps.add("sql_statement");
        uniqueTyps.add("sql_report");
        uniqueTyps.add("Shell");
        uniqueTyps.add("Jobchain");
        uniqueTyps.add("Node");
        return uniqueTyps;
    }
}
