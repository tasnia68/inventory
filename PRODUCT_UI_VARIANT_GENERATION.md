# UI Change Proposal: Variant Auto‑Generation

This document describes how to adapt the UI to auto‑generate variants from attribute options (industry‑standard flow like Shopify).

## 1) Goal
Allow users to define attributes (e.g., Size: S,M,L and Color: Red,Blue) and **generate variants** from combinations, instead of manually creating each SKU.

## 2) What Changes in UI

### A) Attribute Entry
- Keep current attribute creation flow.
- For DROPDOWN/MULTI_SELECT, continue using comma‑separated options.
- After saving attributes, enable a **“Generate Variants”** step.

### B) Variant Generation Step (New UI)
- Input: all attributes for the template with their options.
- Output: all combinations (Cartesian product).
- Show a grid so users can edit:
  - SKU (optional; auto‑generate)
  - Price (required)
  - Barcode (optional)
- Allow:
  - Remove a variant row
  - Bulk edit price
  - Optional: export/import CSV

## 3) How It Maps to Backend

There is **no backend auto‑generation** endpoint. The UI should:
1) Generate combinations client‑side.
2) For each combination, call:
   - POST /api/v1/product-variants (or /api/v1/products)
3) Each request includes:
   - templateId
   - price
   - sku (optional; backend generates if omitted)
   - attributeValues[] (attributeId + value)

Example request:
{
  "templateId": "<template-uuid>",
  "price": 29.99,
  "attributeValues": [
    {"attributeId": "<attr-color>", "value": "Red"},
    {"attributeId": "<attr-size>", "value": "L"}
  ]
}

## 4) UI Validation Rules
- Ensure required attributes are included in each variant.
- Validate values against options list.
- Show warnings for duplicate SKUs before saving.
- If barcode is empty, send `null` or omit it.

## 5) Suggested UI Flow
1) Create Template
2) Create Attributes (with options)
3) Click “Generate Variants”
4) Review grid → confirm
5) Bulk create variants (batch of POST calls)

## 6) Optional Enhancements
- **SKU Template**: e.g., `TSHIRT-{Color}-{Size}`
- **Price rules**: base price + modifiers per option
- **Save draft**: preview combinations before saving

## 7) Error Handling
- If any create call fails, mark the row with error.
- Allow retry for failed rows.
- Show summary: created / failed counts.

## 8) Checklist for UI Adoption
- [ ] Add “Generate Variants” button after attributes saved
- [ ] Build combination generator (cartesian product)
- [ ] Implement grid editor (price/sku/barcode)
- [ ] POST variants sequentially or in batches
- [ ] Display results and errors

---

If you want backend support for bulk creation, we can add a batch endpoint to reduce API calls.
