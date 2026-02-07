# Product Catalog UI Implementation Guide

This document describes how to implement the UI for Product Templates, Product Attributes, and Attribute Groups, based on the backend behavior in this workspace. It focuses on relationships, required inputs, validation, and API usage order.

## 1) Key Relationships (What Maps to What)

### Product Template
- Master product definition (name, description, category, UOM, tracking flags).
- One template → many variants (SKUs).
- Template is required for attributes and variants.

### Product Attribute
- Belongs to a template (`templateId`) and optionally to an attribute group (`groupId`).
- Defines a single characteristic (Color, Size, Material) and validation rules.
- Required attributes must have values on each variant.

### Attribute Group
- Global list (not tied to a template).
- Used to group attributes for UI organization only.

### Category (Related)
- Categories can link to many attributes (category-level defaults).
- Templates belong to a category; UI can show inherited attributes.

## 2) UI Pages & Responsibilities

### A) Attribute Groups Page
**Goal:** Manage global group names used to organize attributes.

**Core actions**
- List groups
- Create group
- View group
- Delete group

**API**
- GET /api/v1/attribute-groups
- POST /api/v1/attribute-groups
- GET /api/v1/attribute-groups/{id}
- DELETE /api/v1/attribute-groups/{id}

**UI Notes**
- Keep it simple: name + description.
- Do not require a template selection.

---

### B) Templates Page
**Goal:** Manage master product definitions.

**Core actions**
- List templates (paged)
- Create template
- Edit template
- View template
- Delete template

**API**
- GET /api/v1/product-templates (paged)
- POST /api/v1/product-templates
- PUT /api/v1/product-templates/{id}
- GET /api/v1/product-templates/{id}
- DELETE /api/v1/product-templates/{id}

**UI Notes**
- Template create/edit requires `categoryId` and `uomId`.
- Template edit should not be blocked by missing variants.
- Add navigation to “Attributes” page scoped to the template.

---

### C) Attributes Page (Template-Scoped)
**Goal:** Define attributes that belong to a specific template.

**Core actions**
- Select a template (required)
- List attributes by template
- Create attribute
- Edit attribute
- Delete attribute

**API**
- GET /api/v1/product-attributes?templateId={templateId}
- POST /api/v1/product-attributes
- PUT /api/v1/product-attributes/{id}
- GET /api/v1/product-attributes/{id}
- DELETE /api/v1/product-attributes/{id}

**UI Notes**
- The list API requires `templateId`. Show a template selector first.
- Allow optional `groupId` for organization.
- Support validation rules based on `type`, `options`, and `validationRegex`.

---

### D) Variants Page (SKU-Level)
**Goal:** Manage sellable SKUs for a template.

**Core actions**
- Create variant
- List variants (optionally filter by template)
- Update variant
- Delete variant

**API (alias endpoints)**
- POST /api/v1/product-variants
- GET /api/v1/product-variants?templateId={templateId}
- GET /api/v1/product-variants/{id}
- PUT /api/v1/product-variants/{id}
- DELETE /api/v1/product-variants/{id}

**UI Notes**
- Required attributes must be provided.
- Values are validated against attribute `type`, `options`, and `validationRegex`.

---

## 3) Attribute Creation & Validation Rules (UI Enforcement)

**Required fields**
- `name` (string)
- `type` (enum: TEXT, NUMBER, DATE, DROPDOWN, MULTI_SELECT)
- `templateId` (UUID)

**Optional fields**
- `required` (boolean)
- `groupId` (UUID)
- `validationRegex` (string)
- `options` (string; comma-separated allowed values for DROPDOWN/MULTI_SELECT)

**Validation to enforce in UI**
- If `type` is DROPDOWN or MULTI_SELECT → `options` must be provided.
- If `validationRegex` is provided → value must match pattern during variant creation.
- Required attributes must be present in variants.

---

## 4) Category-Linked Attributes (Recommended UI Behavior)

Backend supports attaching attributes to categories via `attributeIds` on categories. This enables inherited attributes by category.

**Recommended UX**
- When selecting a template category, display inherited attributes (read-only or editable copy).
- Offer an action like “Add from Category” that creates template attributes based on those IDs.

**API**
- GET /api/v1/categories or /api/v1/categories/tree
- Category response includes `attributeIds`

---

## 5) Suggested UI Flow (Variants Use-Case)

1. Create Template
2. Upload Images (optional)
3. Add Attributes for Template
4. Create Variants and provide values for all required attributes

This matches backend rules where variants validate required attributes and value constraints.

---

## 6) Minimal Data Models for UI

### Template List Item
- id
- name
- description
- categoryId + categoryName
- uomId + uomName
- isActive

### Attribute List Item
- id
- name
- type
- required
- options
- validationRegex
- groupId
- templateId

### Attribute Group
- id
- name
- description

---

## 7) Error Handling UX

- If attributes list returns 400 “Please provide templateId”, show a template selector and retry.
- If attribute create/update fails due to missing template/group, show a clear dropdown validation.
- If variant creation fails due to missing required attribute, highlight required fields.

---

## 8) Example Request Payloads

**Create Attribute Group**
{
  "name": "Dimensions",
  "description": "Physical measurements"
}

**Create Product Attribute**
{
  "name": "Color",
  "type": "DROPDOWN",
  "required": true,
  "options": "Red,Blue,Green,Black",
  "templateId": "<template-uuid>",
  "groupId": "<group-uuid>"
}

**Get Attributes by Template**
GET /api/v1/product-attributes?templateId=<template-uuid>

---

## 9) Final Implementation Checklist

- [ ] Attribute Groups page CRUD works without template selection
- [ ] Templates page CRUD with category/UOM lookup
- [ ] Attributes page requires template selection
- [ ] Attribute type-driven validation in UI
- [ ] Category attribute IDs displayed as inherited suggestions
- [ ] Variant creation enforces required attributes
