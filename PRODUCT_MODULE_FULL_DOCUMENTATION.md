# Product Module — Full Documentation

This document describes business rules, data model, and API usage for the Product module.

## 1) Overview
The Product module uses a **Template → Variant** model:
- **Product Template** = master product definition (name, category, UOM, tracking flags, images).
- **Product Variant** = sellable SKU (price, barcode, attribute values) tied to one template.

## 2) Core Concepts

### 2.1 Product Template
**Purpose:** Defines a product family.
**Fields:**
- name (required)
- description
- categoryId (required)
- uomId (required)
- isActive
- isBatchTracked / isSerialTracked

**Rules:**
- A template can exist without variants.
- Deleting a template deletes its variants (cascade).

### 2.2 Product Variant (SKU)
**Purpose:** A sellable item with a specific attribute combination.
**Fields:**
- templateId (required)
- sku (optional; auto‑generated if not provided)
- price (required)
- barcode (optional, unique if provided)
- attributeValues (required for template attributes marked `required`)

**Rules:**
- SKU must be unique.
- Barcode must be unique when provided.
- Required attributes must be supplied.
- Attribute values are validated (type, options, regex).

### 2.3 Product Attributes
**Purpose:** Define characteristics (Color, Size, Material).
**Fields:**
- name (required)
- type: TEXT | NUMBER | DATE | DROPDOWN | MULTI_SELECT
- required (boolean)
- options (comma‑separated) for DROPDOWN/MULTI_SELECT
- validationRegex (optional)
- templateId (required)
- groupId (optional)

**Rules:**
- Attributes belong to a template.
- Required attributes must be set on all variants.
- Dropdown/multi‑select values must be in options list.

### 2.4 Attribute Groups
**Purpose:** UI organization only (e.g., “Dimensions”, “Tech Specs”).
**Fields:** name, description.

### 2.5 Categories & Attribute Inheritance
Categories can store a list of `attributeIds`. UI can display these as inherited defaults for templates under that category.

## 3) API Reference (Key Endpoints)

### 3.1 Templates
- POST /api/v1/product-templates
- GET /api/v1/product-templates
- GET /api/v1/product-templates/{id}
- PUT /api/v1/product-templates/{id}
- DELETE /api/v1/product-templates/{id}

### 3.2 Attributes
- POST /api/v1/product-attributes
- GET /api/v1/product-attributes?templateId={templateId}
- GET /api/v1/product-attributes/{id}
- PUT /api/v1/product-attributes/{id}
- DELETE /api/v1/product-attributes/{id}

### 3.3 Attribute Groups
- POST /api/v1/attribute-groups
- GET /api/v1/attribute-groups
- GET /api/v1/attribute-groups/{id}
- DELETE /api/v1/attribute-groups/{id}

### 3.4 Variants (Primary)
- POST /api/v1/products
- GET /api/v1/products
- GET /api/v1/products/{id}
- PUT /api/v1/products/{id}
- DELETE /api/v1/products/{id}

### 3.5 Variants (Alias)
- POST /api/v1/product-variants
- GET /api/v1/product-variants
- GET /api/v1/product-variants/{id}
- PUT /api/v1/product-variants/{id}
- DELETE /api/v1/product-variants/{id}
- GET /api/v1/product-variants?templateId={templateId}

### 3.6 Images
- POST /api/v1/product-templates/{templateId}/images
- GET /api/v1/product-templates/{templateId}/images
- GET /api/v1/product-images/{id}/file
- DELETE /api/v1/product-images/{id}
- PUT /api/v1/product-images/{id}/main

## 4) Business Workflows

### 4.1 Simple Product (No Variants)
Use when product has no variations.
1) POST /api/v1/products/simple

### 4.2 Variant Products
1) Create template
2) Upload images
3) Create template attributes
4) Create variants with attribute values

## 5) Validation & Errors
- Missing required attributes → 400
- Invalid option value → 400
- Duplicate SKU → 400
- Duplicate barcode (non‑null) → 400

## 6) UI Implementation Notes
- Attributes page must be template‑scoped.
- Attribute groups are optional and only for UI organization.
- Variants require attribute values for required attributes.
- Use the image file endpoint for viewing images (not frontend URLs).

## 7) Example Payloads

**Create Attribute**
{
  "name": "Color",
  "type": "DROPDOWN",
  "required": true,
  "options": "Red,Blue,Green",
  "templateId": "<template-uuid>",
  "groupId": "<group-uuid>"
}

**Create Variant**
{
  "sku": "TSHIRT-RED-L",
  "price": 29.99,
  "templateId": "<template-uuid>",
  "attributeValues": [
    {"attributeId": "<attr-color>", "value": "Red"},
    {"attributeId": "<attr-size>", "value": "L"}
  ]
}
