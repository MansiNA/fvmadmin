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
    //    rootProjects.forEach(project -> System.out.println("Root Project: " + project.toString()));

        return rootProjects;
    }

    public List<JobManager> getChildJobManager(JobManager parent) {

        List<JobManager> childProjects = jobManagerList
                .stream()
                .filter(sqlDef -> Objects.equals(sqlDef.getPid(), parent.getId()))
                .collect(Collectors.toList());

        // Log the names of child projects
    //    childProjects.forEach(project -> System.out.println("Child Project of " + parent.getName() + ": " + project.getName()));

        return childProjects;
    }

    public JobManager getParentJobManager(JobManager child) {
        if (child.getPid() != null && child.getPid() > 0) {
            Optional<JobManager> parent = jobDefinitionRepository.findById(child.getPid());
            return parent.orElse(null);  // Return the parent if found, or null if not
        } else {
            return null;
        }
    }

    public List<JobManager> getFilteredJobManagers() {
        List<JobManager> listOfJobmanager = new ArrayList<>();
        listOfJobmanager.addAll(jobManagerList);
        List<JobManager> filteredJobManagers = new ArrayList<>();
        // Collect nodes and job chains separately
        List<JobManager> nodeList = listOfJobmanager.stream()
                .filter(jobManager -> "Node".equals(jobManager.getTyp()))
                .collect(Collectors.toList());

        for (JobManager jobManager : nodeList) {
            listOfJobmanager.remove(jobManager);
            listOfJobmanager.removeAll(getJobchainList(jobManager));
        }
        nodeList = filterActiveJobManagers(nodeList);
        for (JobManager jobManager : nodeList) {
            filteredJobManagers.addAll(getJobchainList(jobManager));
        }

        // get all active   children for each jobchain
        List<JobManager> jobChainList = listOfJobmanager.stream()
                  .filter(jobManager -> "Jobchain".equals(jobManager.getTyp()))
                .collect(Collectors.toList());
        for(JobManager jobManager : jobChainList) {
            listOfJobmanager.removeAll(getJobchainList(jobManager));
        }
        filteredJobManagers.addAll(filterActiveJobManagers(jobChainList));

        listOfJobmanager.removeAll(filteredJobManagers);
        for (JobManager jobManager : listOfJobmanager) {
            // For all other job types, collect if aktiv == 1
            if (jobManager.getAktiv() == 1) {
                filteredJobManagers.add(jobManager);
            }
        }

//        filteredJobManagers = filteredJobManagers.stream()
//                .distinct()  // Removes duplicates
//                .collect(Collectors.toList());

        return filteredJobManagers;
    }

    public List<JobManager> filterActiveJobManagers(List<JobManager> jobManagers) {
        return jobManagers.stream()
                .filter(jobManager -> jobManager.getAktiv() == 1)
                .collect(Collectors.toList());
    }

    // Recursively collect active child jobs for a given parent JobManager
    private List<JobManager> getActiveChildJobManagers(JobManager parent, List<JobManager> listOfJobmanager) {
        return listOfJobmanager.stream()
                .filter(job -> Objects.equals(job.getPid(), parent.getId()) && job.getAktiv() == 1)
                .collect(Collectors.toList());
    }

    private void deleteJobChainChildren(JobManager jobChain, List<JobManager> listOfJobmanager) {
        listOfJobmanager.removeAll(getJobchainList(jobChain));
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
