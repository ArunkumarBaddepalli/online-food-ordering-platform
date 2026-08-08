package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.ModifierGroup;
import com.app.fooddelivery.model.Modifier;
import com.app.fooddelivery.repository.ModifierGroupRepository;
import com.app.fooddelivery.repository.ModifierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing modifiers and modifier groups.
 * Provides endpoints for CRUD operations on modifiers.
 */
@RestController
@RequestMapping("/api/modifiers")
public class ModifierController {

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private ModifierRepository modifierRepository;

    /**
     * Get all modifier groups for a specific food item
     */
    @GetMapping("/food-item/{foodItemId}")
    public ResponseEntity<List<ModifierGroup>> getModifierGroupsByFoodItem(@PathVariable Long foodItemId) {
        List<ModifierGroup> modifierGroups = modifierGroupRepository.findByFoodItemId(foodItemId);
        return ResponseEntity.ok(modifierGroups);
    }

    /**
     * Create a new modifier group
     */
    @PostMapping("/groups")
    public ResponseEntity<ModifierGroup> createModifierGroup(@RequestBody ModifierGroup modifierGroup) {
        ModifierGroup saved = modifierGroupRepository.save(modifierGroup);
        return ResponseEntity.ok(saved);
    }

    /**
     * Update a modifier group
     */
    @PutMapping("/groups/{id}")
    public ResponseEntity<ModifierGroup> updateModifierGroup(
            @PathVariable Long id,
            @RequestBody ModifierGroup modifierGroup) {
        modifierGroup.setId(id);
        ModifierGroup updated = modifierGroupRepository.save(modifierGroup);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a modifier group
     */
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteModifierGroup(@PathVariable Long id) {
        modifierGroupRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Create a new modifier
     */
    @PostMapping
    public ResponseEntity<Modifier> createModifier(@RequestBody Modifier modifier) {
        Modifier saved = modifierRepository.save(modifier);
        return ResponseEntity.ok(saved);
    }

    /**
     * Update a modifier
     */
    @PutMapping("/{id}")
    public ResponseEntity<Modifier> updateModifier(
            @PathVariable Long id,
            @RequestBody Modifier modifier) {
        modifier.setId(id);
        Modifier updated = modifierRepository.save(modifier);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a modifier
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModifier(@PathVariable Long id) {
        modifierRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
