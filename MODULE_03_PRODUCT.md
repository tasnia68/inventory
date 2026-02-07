# Module 03: Product & Catalog Management

## Table of Contents
1. **Introduction** - Overview and quick start
2. **Business Logic & Rules** - Data model, constraints, and validation rules
3. **UI/UX User Flows** - Step-by-step user interaction guides with wireframes
4. **API Reference** - Complete REST API documentation
5. **Complete Workflow Examples** - End-to-end scenarios
6. **Decision Guide** - When to use which approach

---

## 1. Introduction
The Product Module allows for the comprehensive management of the product catalog. It adopts a **Template-Variant** architecture (similar to Shopify) where a "Product Template" defines the shared properties, and "Product Variants" represent individual sellable SKUs (e.g., specific sizes or colors).

### Quick Start Guide

**For Simple Products (No Variants):**
```
POST /api/v1/products/simple
```
One API call creates both template and variant. Perfect for unique products like laptops, furniture, etc.

**For Products with Variants:**
```
1. POST /api/v1/product-templates        (Create template)
2. POST /api/v1/product-attributes        (Define Color, Size, etc.)
3. POST /api/v1/product-variants          (Create SKU variants)
```
Multi-step process for products with color/size options like clothing, accessories, etc.

---

## 2. Business Logic & Rules

### 2.1 Core Concepts

#### Product Template
- **Purpose**: Represents the "master product" or product family
- **Contains**: Name, description, category, UOM, tracking settings, images
- **Shared by**: All variants of the same product
- **Example**: "Premium T-Shirt" (the general product)

#### Product Variant
- **Purpose**: Represents a specific sellable SKU
- **Contains**: SKU code, barcode, price, specific attribute values
- **Relationship**: Must belong to exactly one template (mandatory)
- **Example**: "TSHIRT-RED-L" (specific size/color combination)

#### Product Attributes
- **Purpose**: Define characteristics that differentiate variants
- **Types**: TEXT, NUMBER, DATE, DROPDOWN, MULTI_SELECT
- **Can be**: Required or optional
- **Scope**: 
  - Template-specific (applies to one product)
  - Category-level (inherited by all products in category)
  - Reusable across multiple templates

#### Attribute Values
- **Purpose**: Store the specific value of an attribute for a variant
- **Example**: For variant "TSHIRT-RED-L", attribute values are Color=Red, Size=L
- **Validation**: Must match attribute type and allowed options

### 2.2 Business Rules & Constraints

#### Template Rules:
1. ✅ Template name is **required**
2. ✅ Must have a **category** assigned
3. ✅ Must have a **UOM** (Unit of Measure) assigned
4. ✅ Can exist without variants (but variants need a template)
5. ✅ Images are stored at template level, shared by all variants
6. ⚠️ Deleting a template deletes all its variants (cascade delete)

#### Variant Rules:
1. ✅ SKU must be **unique** across the entire system
2. ✅ Must be linked to a **template** (cannot exist independently)
3. ✅ Barcode is optional but must be unique if provided
4. ✅ Price is **required** and must be positive
5. ✅ Must provide values for all **required attributes** of the template
6. ⚠️ Attribute values must match the attribute type (e.g., dropdown value must be in allowed options)

#### Attribute Rules:
1. ✅ Attribute name is required
2. ✅ Type must be one of: TEXT, NUMBER, DATE, DROPDOWN, MULTI_SELECT
3. ✅ DROPDOWN/MULTI_SELECT types must have predefined options
4. ✅ Can have regex validation for TEXT/NUMBER types
5. ✅ Required attributes must have values in all variants
6. ⚠️ Deleting an attribute removes it from all variants

#### SKU Generation:
- If SKU is not provided, system auto-generates: `PREFIX-XXXXXXXX`
- Prefix derived from template name (first 3 characters)
- Followed by 8-character random UUID substring

### 2.3 Data Model Relationships

```
┌─────────────────────┐
│  ProductTemplate    │
│  - name             │
│  - description      │◄──────┐
│  - category         │       │
│  - uom              │       │ Many
│  - images[]         │       │
└──────────┬──────────┘       │
           │                  │
           │ One              │
           │                  │
           ▼                  │
┌─────────────────────┐       │
│  ProductAttribute   │───────┘
│  - name             │
│  - type             │
│  - required         │
│  - options          │
└──────────┬──────────┘
           │
           │ One
           │
           ▼
┌─────────────────────────────┐
│  ProductAttributeValue      │
│  - value                    │
└─────────────┬───────────────┘
              │
              │ Many
              │
              ▼
┌─────────────────────┐
│  ProductVariant     │
│  - sku              │
│  - barcode          │
│  - price            │
└─────────────────────┘
```

**Relationship Summary:**
- Template (1) → Attributes (Many)
- Template (1) → Variants (Many)
- Attribute (1) → Attribute Values (Many)
- Variant (1) → Attribute Values (Many)

**Example:**
```
Template: "Premium T-Shirt"
  ├─ Attributes:
  │   ├─ Color (DROPDOWN: Red, Blue, Green)
  │   └─ Size (DROPDOWN: S, M, L, XL)
  │
  └─ Variants:
      ├─ SKU: TSHIRT-RED-L, Price: $29.99
      │   ├─ Color = Red
      │   └─ Size = L
      │
      └─ SKU: TSHIRT-BLUE-M, Price: $27.99
          ├─ Color = Blue
          └─ Size = M
```

---

## 3. UI/UX User Flows

### 3.1 Setup Flow - Catalog Configuration (One-Time)

**User Role:** Admin/Manager

**Step 1: Create Categories**
```
Navigate to: Settings → Categories
Action: Click "Add Category"
Input: Name = "Electronics", Parent = None
Action: Click "Add Subcategory" 
Input: Name = "Laptops", Parent = "Electronics"
Result: Category tree created
```

**Step 2: Define Units of Measure**
```
Navigate to: Settings → Units of Measure
Action: Click "Add UOM"
Input: Name = "Piece", Code = "pc", Type = "QUANTITY"
Result: Base UOM created
```

**Step 3: Create Attribute Groups (Optional)**
```
Navigate to: Settings → Attribute Groups
Action: Click "Create Group"
Input: Name = "Dimensions", Description = "Physical measurements"
Result: Group created for organizing attributes
```

**Step 4: Define Reusable Attributes**
```
Navigate to: Settings → Product Attributes
Action: Click "Create Attribute"
Form Fields:
  - Name: "Color"
  - Type: Dropdown
  - Options: "Red, Blue, Green, Black, White"
  - Required: Yes
  - Link to: Category "Clothing" (optional)
Result: Attribute available for all clothing products
```

### 3.2 Flow A: Create Simple Product (No Variants)

**User Role:** Manager/Admin  
**Use Case:** Adding a unique product like a laptop with serial number

**UI Wireframe Flow:**
```
Page: Products → Click "Add Simple Product"

┌─────────────────────────────────────┐
│  Create Simple Product              │
├─────────────────────────────────────┤
│  Product Name: [Dell XPS 15      ]  │
│  Description:  [High-performance...]│
│  Category:     [▼ Electronics    ]  │
│  UOM:          [▼ Piece          ]  │
│                                     │
│  SKU:          [DELL-XPS15-001   ]  │
│  Barcode:      [9876543210       ]  │
│  Price:        [$1,299.99        ]  │
│                                     │
│  ☑ Serial Tracked                  │
│  ☐ Batch Tracked                   │
│                                     │
│  [Cancel]           [Create Product]│
└─────────────────────────────────────┘
```

**Step-by-Step:**
1. User clicks **"Add Simple Product"** button
2. Fills in product details in single form
3. System validates SKU uniqueness
4. Clicks **"Create Product"**
5. System creates template + variant automatically
6. Redirects to product detail page

**System Actions:**
- Creates ProductTemplate with entered details
- Creates ProductVariant with SKU/price
- Links variant to template
- Product immediately available for sale

### 3.3 Flow B: Create Product with Variants

**User Role:** Manager/Admin  
**Use Case:** Adding clothing item with multiple colors and sizes

#### Step 1: Create Product Template

```
Page: Products → Click "Add Product Template"

┌─────────────────────────────────────┐
│  Create Product Template            │
├─────────────────────────────────────┤
│  Product Name: [Premium T-Shirt  ]  │
│  Description:  [100% organic...  ]  │
│  Category:     [▼ Clothing       ]  │
│  UOM:          [▼ Piece          ]  │
│                                     │
│  Tracking:                          │
│  ☐ Serial Tracked                  │
│  ☐ Batch Tracked                   │
│                                     │
│  [Cancel]              [Next: Images]│
└─────────────────────────────────────┘
```

#### Step 2: Upload Images

```
Page: Product Images

┌─────────────────────────────────────┐
│  Upload Product Images              │
├─────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐           │
│  │[Image 1]│  │[Image 2]│  [+ Add]  │
│  │  ⭐Main │  │         │           │
│  └─────────┘  └─────────┘           │
│                                     │
│  Drag & drop or click to upload     │
│                                     │
│  [Back]           [Next: Attributes]│
└─────────────────────────────────────┘
```

**Actions:**
- Upload multiple images
- Select one as "Main Image" (shown in listings)
- System stores in MinIO, links to template

#### Step 3: Define Attributes

```
Page: Product Attributes

┌─────────────────────────────────────┐
│  Define Product Attributes          │
├─────────────────────────────────────┤
│  Inherited from Category "Clothing":│
│  ✓ Color (Dropdown)                 │
│  ✓ Size (Dropdown)                  │
│                                     │
│  [+ Add Custom Attribute]           │
│                                     │
│  Or customize:                      │
│  ┌───────────────────────────────┐  │
│  │ Color ▼                      │  │
│  │ Type: Dropdown               │  │
│  │ Options: Red, Blue, Green    │  │
│  │ ☑ Required                   │  │
│  └───────────────────────────────┘  │
│                                     │
│  [Back]           [Next: Variants]  │
└─────────────────────────────────────┘
```

**Actions:**
- View attributes inherited from category
- Add new template-specific attributes
- Configure attribute options and requirements

#### Step 4: Generate Variants

```
Page: Create Variants

┌──────────────────────────────────────────────┐
│  Generate Product Variants                   │
├──────────────────────────────────────────────┤
│  [Auto-Generate from Combinations] or        │
│  [Add Manually]                              │
│                                              │
│  Attributes: Color (3) × Size (4) = 12 SKUs  │
│                                              │
│  ┌─────────────────────────────────────────┐ │
│  │ SKU         Color  Size  Price  Barcode │ │
│  │ TSHIRT-R-S  Red    S     $25.99  [...]  │ │
│  │ TSHIRT-R-M  Red    M     $27.99  [...]  │ │
│  │ TSHIRT-R-L  Red    L     $29.99  [...]  │ │
│  │ TSHIRT-B-S  Blue   S     $25.99  [...]  │ │
│  │ ... (8 more rows)                       │ │
│  └─────────────────────────────────────────┘ │
│                                              │
│  [Bulk Edit Prices] [Import from CSV]       │
│                                              │
│  [Back]                  [Create 12 Variants]│
└──────────────────────────────────────────────┘
```

**Actions:**
- Auto-generate all combinations or add manually
- Edit SKUs, prices, barcodes
- Bulk operations for efficiency
- Create all variants at once

**System Actions:**
- Validates all SKUs are unique
- Validates required attributes are filled
- Creates ProductVariant records
- Creates ProductAttributeValue records
- Products ready for inventory/sales

### 3.4 Flow C: Managing Existing Products

#### View Products List
```
Page: Products

┌────────────────────────────────────────────────┐
│  Products                        [+ Add Product]│
├────────────────────────────────────────────────┤
│  🔍 Search: [           ]  Filter: [All ▼]     │
│                                                │
│  ┌──────────────────────────────────────────┐  │
│  │ 📦 Premium T-Shirt                       │  │
│  │    12 variants • Electronics             │  │
│  │    [$25.99 - $29.99]            [Edit]   │  │
│  ├──────────────────────────────────────────┤  │
│  │ 💻 Dell XPS 15                           │  │
│  │    1 variant • Laptops                   │  │
│  │    [$1,299.99]                  [Edit]   │  │
│  └──────────────────────────────────────────┘  │
└────────────────────────────────────────────────┘
```

#### Edit Variant Pricing
```
Page: Product Detail → Variants Tab

┌────────────────────────────────────────┐
│  Premium T-Shirt - Variants            │
├────────────────────────────────────────┤
│  [+ Add Variant]                       │
│                                        │
│  SKU: TSHIRT-RED-L                     │
│  Attributes: Color=Red, Size=L         │
│  Price: [$29.99] → [$27.99] [Update]  │
│  Stock: 45 units                       │
│                                        │
│  [Delete Variant]                      │
└────────────────────────────────────────┘
```

---

## 4. API Reference

### 4.1 Attribute Groups
**Endpoint:** `/api/v1/attribute-groups`

Attribute Groups are optional organizational containers for grouping related attributes (e.g., "Dimensions", "Technical Specs").

#### Create Attribute Group
**POST** `/api/v1/attribute-groups`
**Request:**
```json
{
  "name": "Dimensions",
  "description": "Physical measurements"
}
```
**Response (201 Created):**
```json
{
  "status": 201,
  "success": true,
  "message": "Attribute group created",
  "data": {
    "id": "a1b2c3d4-...",
    "name": "Dimensions",
    "description": "Physical measurements",
    "createdAt": "2026-01-25T10:00:00",
    "createdBy": "admin@example.com"
  }
}
```

#### Get All Attribute Groups
**GET** `/api/v1/attribute-groups`
**Response (200 OK):**
```json
{
  "status": 200,
  "success": true,
  "message": "Attribute groups retrieved",
  "data": [
    {
      "id": "a1b2c3d4-...",
      "name": "Dimensions",
      "description": "Physical measurements"
    }
  ]
}
```

### 4.2 Category Management
**Endpoint:** `/api/v1/categories`

#### Create Category
**POST** `/api/v1/categories`
**Request:**
```json
{
  "name": "Electronics",
  "description": "Gadgets and Devices",
  "parentId": null 
}
```
**Response (201 Created):**
```json
{
  "status": 201,
  "success": true,
  "data": {
    "id": "c766bbb3-...",
    "name": "Electronics",
    "description": "Gadgets and Devices",
    "children": []
  }
}
```

### 4.3 Unit of Measure (UOM)
**Endpoint:** `/api/v1/uoms`

#### Create Base UOM
**POST** `/api/v1/uoms`
**Request:**
```json
{
  "name": "Piece",
  "code": "pc",
  "category": "QUANTITY",  // Values: WEIGHT, VOLUME, QUANTITY, TIME, LENGTH
  "isBase": true,
  "conversionFactor": 1.0
}
```
**Response (201 Created):**
```json
{
  "status": 201,
  "success": true,
  "data": {
    "id": "e62d5f1a-...",
    "name": "Piece",
    "code": "pc",
    "category": "QUANTITY",
    "isBase": true,
    "conversionFactor": 1.0
  }
}
```

### 4.4 Product Attributes
**Endpoint:** `/api/v1/product-attributes`

Product Attributes define the characteristics that differentiate variants (Color, Size, Material, etc.).

#### Create Product Attribute
**POST** `/api/v1/product-attributes`
**Request:**
```json
{
  "name": "Color",
  "type": "DROPDOWN",
  "required": true,
  "options": "Red,Blue,Green,Black",
  "templateId": "94f4ac4c-...",
  "groupId": "a1b2c3d4-..."
}
```
**Valid Types:** `TEXT`, `NUMBER`, `DATE`, `DROPDOWN`, `MULTI_SELECT`

**Response (201 Created):**
```json
{
  "status": 201,
  "success": true,
  "message": "Product attribute created",
  "data": {
    "id": "attr123-...",
    "name": "Color",
    "type": "DROPDOWN",
    "required": true,
    "options": "Red,Blue,Green,Black",
    "templateId": "94f4ac4c-...",
    "groupId": "a1b2c3d4-...",
    "createdAt": "2026-01-25T10:00:00"
  }
}
```

#### Get Attributes by Template
**GET** `/api/v1/product-attributes?templateId={templateId}`
**Response (200 OK):**
```json
{
  "status": 200,
  "success": true,
  "message": "Product attributes retrieved",
  "data": [
    {
      "id": "attr123-...",
      "name": "Color",
      "type": "DROPDOWN",
      "required": true,
      "options": "Red,Blue,Green,Black",
      "templateId": "94f4ac4c-..."
    },
    {
      "id": "attr456-...",
      "name": "Size",
      "type": "DROPDOWN",
      "required": true,
      "options": "S,M,L,XL",
      "templateId": "94f4ac4c-..."
    }
  ]
}
```

#### Update Product Attribute
**PUT** `/api/v1/product-attributes/{id}`
**Request:**
```json
{
  "name": "Color",
  "type": "DROPDOWN",
  "required": false,
  "options": "Red,Blue,Green,Black,White",
  "templateId": "94f4ac4c-...",
  "groupId": "a1b2c3d4-..."
}
```

### 4.5 Product Template
**Endpoint:** `/api/v1/product-templates`

#### Create Template
**POST** `/api/v1/product-templates`
**Request:**
```json
{
  "name": "Premium T-Shirt",
  "description": "100% Cotton",
  "categoryId": "c766bbb3-...", 
  "uomId": "e62d5f1a-...",
  "isBatchTracked": false,
  "isSerialTracked": false,
  "isActive": true
}
```
**Response (201 Created):**
```json
{
  "status": 201,
  "success": true,
  "data": {
    "id": "94f4ac4c-...",
    "name": "Premium T-Shirt",
    "description": "100% Cotton",
    "isActive": true,
    "categoryId": "c766bbb3-...",
    "categoryName": "Electronics",
    "uomId": "e62d5f1a-...",
    "uomName": "Piece"
  }
}
```

### 4.6 Product Images (MinIO)
**Endpoint:** `/api/v1/product-templates/{templateId}/images`

#### Upload Image
**POST** `/api/v1/product-templates/{templateId}/images`
- **Content-Type**: `multipart/form-data`
- **Parts**:
    - `file`: (Binary file data)
    - `isMain`: `true` or `false`

**Response (201 Created):**
```json
{
  "status": 201,
  "success": true,
  "data": {
    "id": "ea23e675-...",
    "url": "product-images/94f4ac4c.../image.jpg",
    "filename": "image.jpg",
    "isMain": true,
    "productTemplateId": "94f4ac4c-..."
  }
}
```

#### Get Images
**GET** `/api/v1/product-templates/{templateId}/images`
**Response (200 OK):**
```json
{
  "status": 200,
  "success": true,
  "data": [
    {
      "id": "ea23e675-...",
      "url": "product-images/...",
      "filename": "image.jpg",
      "isMain": true
    },
    {
      "id": "fb57a655-...",
      "url": "product-images/...",
      "filename": "back.jpg",
      "isMain": false
    }
  ]
}
```

### 4.7 Product Variants
**Endpoint:** `/api/v1/product-variants`

Product Variants are the actual sellable SKUs with specific attribute values.

#### Create Simple Product (Convenience API)
**POST** `/api/v1/products/simple`

**Use Case:** For products without variants (no color/size options). Creates both template and variant in a single request.

**Request:**
```json
{
  "name": "Basic Laptop Model X",
  "description": "Standard office laptop",
  "categoryId": "c766bbb3-...",
  "uomId": "e62d5f1a-...",
  "sku": "LAPTOP-X-001",
  "barcode": "1234567890123",
  "price": 599.99,
  "isBatchTracked": false,
  "isSerialTracked": true,
  "isActive": true
}
```
**Response (201 Created):**
```json
{
  "status": 201,
  "success": true,
  "message": "Simple product created successfully",
  "data": {
    "id": "variant789-...",
    "sku": "LAPTOP-X-001",
    "barcode": "1234567890123",
    "price": 599.99,
    "templateId": "auto-generated-template-id",
    "attributeValues": [],
    "createdAt": "2026-01-25T10:00:00"
  }
}
```

**Note:** This convenience endpoint automatically creates a product template behind the scenes. Use this when your product doesn't need variants (different colors, sizes, etc.).

---

#### Create Variant (SKU) - For Products with Attributes
**POST** `/api/v1/product-variants`

**Use Case:** For products with variants (different colors, sizes, etc.). Requires an existing template.

**Request:**
```json
{
  "sku": "TSHIRT-RED-L",
  "barcode": "1234567890123",
  "price": 29.99,
  "templateId": "94f4ac4c-...",
  "attributeValues": [
    {
      "attributeId": "attr123-...",
      "value": "Red"
    },
    {
      "attributeId": "attr456-...",
      "value": "L"
    }
  ]
}
```
**Response (201 Created):**
```json
{
  "status": 201,
  "success": true,
  "message": "Product variant created",
  "data": {
    "id": "variant789-...",
    "sku": "TSHIRT-RED-L",
    "barcode": "1234567890123",
    "price": 29.99,
    "templateId": "94f4ac4c-...",
    "attributeValues": [
      {
        "id": "val001-...",
        "attributeId": "attr123-...",
        "attributeName": "Color",
        "value": "Red"
      },
      {
        "id": "val002-...",
        "attributeId": "attr456-...",
        "attributeName": "Size",
        "value": "L"
      }
    ],
    "createdAt": "2026-01-25T10:00:00"
  }
}
```

---

## 5. Complete Workflow Examples

### 5.1 Simple Product (No Variants) - Using Convenience API

**Scenario:** Creating a laptop product without variants

**Single Request:**
```http
POST /api/v1/products/simple
{
  "name": "Dell XPS 15",
  "description": "High-performance laptop with 16GB RAM",
  "categoryId": "category-electronics",
  "uomId": "uom-piece",
  "sku": "DELL-XPS15-001",
  "barcode": "9876543210123",
  "price": 1299.99,
  "isSerialTracked": true,
  "isActive": true
}
```

**Result:**
- ✅ 1 Product Template created automatically
- ✅ 1 Variant (SKU) created and ready to sell
- ✅ No attributes needed
- ✅ Perfect for unique products

---

### 5.2 Product with Variants - Multi-Step Process

**Scenario:** Creating a T-Shirt with multiple color and size options

**Step 1: Create Product Template**
```http
POST /api/v1/product-templates
{
  "name": "Premium T-Shirt",
  "description": "100% Organic Cotton",
  "categoryId": "category-clothing",
  "uomId": "uom-piece",
  "isActive": true
}
// Returns: templateId = "tpl-001"
```

**Step 2: Upload Images**
```http
POST /api/v1/product-templates/tpl-001/images
FormData: file=front.jpg, isMain=true

POST /api/v1/product-templates/tpl-001/images  
FormData: file=back.jpg, isMain=false
```

**Step 3: Create Attributes for Template**
```http
POST /api/v1/product-attributes
{
  "name": "Color",
  "type": "DROPDOWN",
  "required": true,
  "options": "Red,Blue,Green",
  "templateId": "tpl-001"
}
// Returns: attributeId = "attr-color"

POST /api/v1/product-attributes
{
  "name": "Size",
  "type": "DROPDOWN",
  "required": true,
  "options": "S,M,L,XL",
  "templateId": "tpl-001"
}
// Returns: attributeId = "attr-size"
```

**Step 4: Create Variants (SKUs)**
```http
POST /api/v1/product-variants
{
  "sku": "TSHIRT-RED-L",
  "price": 29.99,
  "templateId": "tpl-001",
  "attributeValues": [
    {"attributeId": "attr-color", "value": "Red"},
    {"attributeId": "attr-size", "value": "L"}
  ]
}

POST /api/v1/product-variants
{
  "sku": "TSHIRT-BLUE-M",
  "price": 27.99,
  "templateId": "tpl-001",
  "attributeValues": [
    {"attributeId": "attr-color", "value": "Blue"},
    {"attributeId": "attr-size", "value": "M"}
  ]
}
```

**Result:**
- 1 Product Template: "Premium T-Shirt"
- 2 Attributes: Color, Size  
- 2 Variants: TSHIRT-RED-L ($29.99), TSHIRT-BLUE-M ($27.99)
- Each variant inherits the template's images, description, and category

---

## 6. Decision Guide: Simple Product vs Template-Variant

### Use **Simple Product API** (`POST /api/v1/products/simple`) When:
- ✅ Product has NO variations (no color, size, material options)
- ✅ Each product is unique (e.g., serial-tracked electronics)
- ✅ One-time or low-volume products
- ✅ Want quick setup without managing templates

**Examples:** Laptops with serial numbers, custom machinery, office furniture

### Use **Template-Variant Pattern** When:
- ✅ Product comes in multiple variations (colors, sizes, flavors, etc.)
- ✅ Shared description and images across variants
- ✅ Need to manage product families
- ✅ Bulk operations on related SKUs

**Examples:** Clothing (sizes/colors), beverages (flavors), accessories (styles)
