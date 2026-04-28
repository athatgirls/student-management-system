<template>
  <div class="about">
    <h1>This is an about page</h1>
  </div>
</template>

<style scoped>
.about {
  padding: 20px;
  text-align: center;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .about {
    padding: 15px;
  }

  .about h1 {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .about {
    padding: 10px;
  }

  .about h1 {
    font-size: 18px;
  }
}
</style>
