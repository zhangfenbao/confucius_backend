export default function authHeaders() {
  return {
    Authorization: `Bearer ${process.env.SESAME_USER_TOKEN}`,
  };
}
