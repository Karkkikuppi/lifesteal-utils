FROM oven/bun:latest

WORKDIR /app

COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

COPY . .

EXPOSE 3030
EXPOSE 9090

CMD ["bun", "index.ts"]
