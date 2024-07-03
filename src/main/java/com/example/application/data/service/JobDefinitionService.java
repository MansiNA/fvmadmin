package com.example.application.data.service;

import com.example.application.data.entity.JobDefinition;
import com.example.application.data.entity.SqlDefinition;
import com.example.application.data.repository.JobDefinitionRepository;
import com.example.application.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JobDefinitionService {

    private final JobDefinitionRepository jobDefinitionRepository;
    private List<JobDefinition> jobDefinitionList;

    @Autowired
    public JobDefinitionService(JobDefinitionRepository jobDefinitionRepository) {
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.jobDefinitionList = jobDefinitionRepository.findAll();
    }

    public List<JobDefinition> findAll() {
        jobDefinitionList = jobDefinitionRepository.findAll();
        return jobDefinitionList;
    }

    public Optional<JobDefinition> findById(Integer id) {
        return jobDefinitionRepository.findById(id);
    }

    public JobDefinition save(JobDefinition jobDefinition) {
        return jobDefinitionRepository.save(jobDefinition);
    }

    public void deleteById(Integer id) {
        jobDefinitionRepository.deleteById(id);
    }

    public List<JobDefinition> getRootProjects() {
        System.out.println("-----------"+jobDefinitionList.size()+"-----------------------------");
        List<JobDefinition> rootProjects = jobDefinitionList
                .stream()
                .filter(sqlDef -> sqlDef.getPid() == 0)
                .collect(Collectors.toList());

        // Log the names of root projects
        rootProjects.forEach(project -> System.out.println("Root Project: " + project.getName()));

        return rootProjects;
    }

    public List<JobDefinition> getChildProjects(JobDefinition parent) {

        List<JobDefinition> childProjects = jobDefinitionList
                .stream()
                .filter(sqlDef -> Objects.equals(sqlDef.getPid(), parent.getId()))
                .collect(Collectors.toList());

        // Log the names of child projects
        childProjects.forEach(project -> System.out.println("Child Project of " + parent.getName() + ": " + project.getName()));

        return childProjects;
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
}
